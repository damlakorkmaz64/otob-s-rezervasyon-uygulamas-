from datetime import datetime
from typing import Optional, List

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import declarative_base, sessionmaker, relationship

app = FastAPI(title="MobileProject Backend")

# --- DB ---
engine = create_engine("sqlite:///mobileproject.db", connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)
Base = declarative_base()

class UserDB(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True)
    username = Column(String, unique=True, index=True, nullable=False)
    password = Column(String, nullable=False)  # demo için düz string (sunum yeterli)
    role = Column(String, nullable=False, default="user")  # "user" / "admin"
    created_at = Column(DateTime, default=datetime.utcnow)

class SeferDB(Base):
    __tablename__ = "seferler"
    id = Column(Integer, primary_key=True)
    frm = Column(String, nullable=False)
    to = Column(String, nullable=False)
    date = Column(String, nullable=False)
    time = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class ReservationDB(Base):
    __tablename__ = "reservations"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    sefer_id = Column(Integer, ForeignKey("seferler.id"), nullable=False)
    seat_number = Column(Integer, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("UserDB")
    sefer = relationship("SeferDB")

Base.metadata.create_all(bind=engine)

# --- Schemas ---
class RegisterIn(BaseModel):
    username: str
    password: str
    role: str = "user"  # user/admin

class LoginIn(BaseModel):
    username: str
    password: str

class UserOut(BaseModel):
    id: int
    username: str
    role: str

class SeferIn(BaseModel):
    from_: str
    to: str
    date: str
    time: str

class SeferOut(BaseModel):
    id: int
    from_: str
    to: str
    date: str
    time: str

class ReservationIn(BaseModel):
    user_id: int
    sefer_id: int
    seat_number: int

class ReservationOut(BaseModel):
    id: int
    user_id: int
    sefer_id: int
    seat_number: int
    created_at: str

def db():
    return SessionLocal()

# --- Health ---
@app.get("/")
def health():
    return {"status": "ok", "message": "Backend çalışıyor"}
# --- SEED (hazır kullanıcı + sefer) ---
def seed_if_empty():
    s = SessionLocal()
    try:
        # 1) Kullanıcı seed
        user_count = s.query(UserDB).count()
        if user_count == 0:
            admin = UserDB(username="admin", password="1234", role="admin")
            user = UserDB(username="user", password="1234", role="user")
            s.add_all([admin, user])
            s.commit()

        # 2) Sefer seed
        sefer_count = s.query(SeferDB).count()
        if sefer_count == 0:
            demo_seferler = [
                SeferDB(frm="İzmir", to="Ankara", date="25.12.2025", time="14:30"),
                SeferDB(frm="İstanbul", to="Bursa", date="26.12.2025", time="09:00"),
                SeferDB(frm="Ankara", to="İzmir", date="27.12.2025", time="18:45"),
                SeferDB(frm="İzmir", to="İstanbul", date="28.12.2025", time="12:15"),
                SeferDB(frm="Bursa", to="Ankara", date="29.12.2025", time="20:00"),
            ]
            s.add_all(demo_seferler)
            s.commit()
    finally:
        s.close()


@app.on_event("startup")
def on_startup():
    seed_if_empty()

# --- Auth ---
@app.post("/auth/register", response_model=UserOut)
def register(body: RegisterIn):
    s = db()
    try:
        if body.role not in ("user", "admin"):
            raise HTTPException(status_code=400, detail="role user/admin olmalı")

        exists = s.query(UserDB).filter(UserDB.username == body.username).first()
        if exists:
            raise HTTPException(status_code=409, detail="username alınmış")

        u = UserDB(username=body.username, password=body.password, role=body.role)
        s.add(u)
        s.commit()
        s.refresh(u)
        return UserOut(id=u.id, username=u.username, role=u.role)
    finally:
        s.close()

@app.post("/auth/login", response_model=UserOut)
def login(body: LoginIn):
    s = db()
    try:
        u = s.query(UserDB).filter(UserDB.username == body.username).first()
        if not u or u.password != body.password:
            raise HTTPException(status_code=401, detail="Hatalı giriş")
        return UserOut(id=u.id, username=u.username, role=u.role)
    finally:
        s.close()

# --- Seferler ---
@app.get("/seferler", response_model=List[SeferOut])
def list_seferler():
    s = db()
    try:
        items = s.query(SeferDB).order_by(SeferDB.id.desc()).all()
        return [SeferOut(id=x.id, from_=x.frm, to=x.to, date=x.date, time=x.time) for x in items]
    finally:
        s.close()

@app.post("/admin/seferler", response_model=SeferOut)
def add_sefer(body: SeferIn):
    s = db()
    try:
        x = SeferDB(frm=body.from_, to=body.to, date=body.date, time=body.time)
        s.add(x)
        s.commit()
        s.refresh(x)
        return SeferOut(id=x.id, from_=x.frm, to=x.to, date=x.date, time=x.time)
    finally:
        s.close()


@app.delete("/admin/seferler/{sefer_id}")
def delete_sefer(sefer_id: int):
    s = db()
    try:
        x = s.query(SeferDB).filter(SeferDB.id == sefer_id).first()
        if not x:
            raise HTTPException(status_code=404, detail="sefer bulunamadı")

        # önce bu sefere ait rezervasyonları sil
        s.query(ReservationDB).filter(ReservationDB.sefer_id == sefer_id).delete()

        s.delete(x)
        s.commit()
        return {"deleted": True}
    finally:
        s.close()

# --- Rezervasyon ---
@app.post("/reservations", response_model=ReservationOut)
def create_reservation(body: ReservationIn):
    s = db()
    try:
        # user/sefer var mı?
        u = s.query(UserDB).filter(UserDB.id == body.user_id).first()
        if not u:
            raise HTTPException(status_code=404, detail="user yok")
        sefer = s.query(SeferDB).filter(SeferDB.id == body.sefer_id).first()
        if not sefer:
            raise HTTPException(status_code=404, detail="sefer yok")

        # aynı sefer + koltuk dolu mu?
        exists = s.query(ReservationDB).filter(
            ReservationDB.sefer_id == body.sefer_id,
            ReservationDB.seat_number == body.seat_number
        ).first()
        if exists:
            raise HTTPException(status_code=409, detail="Koltuk dolu")

        r = ReservationDB(user_id=body.user_id, sefer_id=body.sefer_id, seat_number=body.seat_number)
        s.add(r)
        s.commit()
        s.refresh(r)
        return ReservationOut(
            id=r.id,
            user_id=r.user_id,
            sefer_id=r.sefer_id,
            seat_number=r.seat_number,
            created_at=r.created_at.isoformat()
        )
    finally:
        s.close()

@app.get("/reservations/user/{user_id}", response_model=List[ReservationOut])
def list_reservations(user_id: int):
    s = db()
    try:
        items = s.query(ReservationDB).filter(ReservationDB.user_id == user_id).order_by(ReservationDB.id.desc()).all()
        return [
            ReservationOut(
                id=r.id, user_id=r.user_id, sefer_id=r.sefer_id,
                seat_number=r.seat_number, created_at=r.created_at.isoformat()
            )
            for r in items
        ]
    finally:
        s.close()
@app.delete("/reservations/{reservation_id}")
def delete_reservation(reservation_id: int):
    s = db()
    try:
        r = s.query(ReservationDB).filter(ReservationDB.id == reservation_id).first()
        if not r:
            raise HTTPException(status_code=404, detail="rezervasyon bulunamadı")

        s.delete(r)
        s.commit()
        return {"deleted": True, "reservation_id": reservation_id}
    finally:
        s.close()

@app.get("/reservations/sefer/{sefer_id}/seats")
def get_reserved_seats(sefer_id: int):
    s = db()
    try:
        seats = (
            s.query(ReservationDB.seat_number)
            .filter(ReservationDB.sefer_id == sefer_id)
            .all()
        )
        # [(12,), (7,), ...] -> [12, 7, ...]
        return {"sefer_id": sefer_id, "reserved_seats": [x[0] for x in seats]}
    finally:
        s.close()


