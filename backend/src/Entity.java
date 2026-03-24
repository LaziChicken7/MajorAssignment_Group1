import java.time.LocalDateTime;


public abstract class Entity {
    protected String id; //Moi mot doi tuong (ke ca user va ke ca san pham deu phai co id rieng)
    protected LocalDateTime createdAt; //Thoi diem thuc the duoc tao ra trong he thong
    protected LocalDateTime updatedAt; //Thoi diem thuc the thay doi trong he thong
    protected boolean isActived; // Thay vi xoa han du lieu khoi database thi isActived chi de an no di, co the enable lai neu muon

    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        isActived = true;
    }

    abstract public void displayInfo();
    //Bat buoc phai viet thong tin cua thuc the de debug hoac cho admin biet (co the dung System.err.println() (la gi thi tu nghien cuu))
    abstract public boolean isValid();

    public void update() {
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isActived = false;
        this.update();
        ///  ////////////
    }
}

/*
createdAt va updatedAt dung de luu log
Mot server admin nen co log de quan li user va transaction duoc ro rang hon de kiem tra co sai pham trong giao dich hay khong
*/

/*
? Tu dung quen mat dinh viet gi roi =))
Password: Tim kiem bang HashMap chu khong for binh thuong (de ctrinh chay nhanh hon)
*/

/*
Quy dinh trong cach van hanh id (moi id trong User, Product hay Transaction deu phai unique):
xxx: 001, 002, ...

User:
Khi moi tao tai khoan: USRxxx
Khi duoc cap quyen Bidder: BIDxxx
Khi duoc cap quyen Seller: SLRxxx
Khi duoc cap ca hai quyen Bidder va Seller: UVSxxx (universal)
Khi duoc cap quyen admin: ADMxxx
*/