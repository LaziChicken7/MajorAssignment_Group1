
interface verifyCitizenID {
    public void updateCitizenID(String citizenID);
}


public class User extends Entity implements verifyCitizenID {
    protected final String userName;
    protected String realName, citizenID;
    protected double moneyOnWallet;
    protected boolean isBidder;
    protected boolean isSeller;
    protected boolean isAdmin;
    private String encodingPassword;
    
    public User(String id, String userName, String realName, String encodingPassword, boolean isBidder, boolean isSeller, boolean isAdmin) {
        super(id);
        this.userName = userName;
        this.realName = realName;
        this.encodingPassword = encodingPassword;
        this.moneyOnWallet = 0;
        this.isBidder = isBidder;
        this.isSeller = isSeller;
        this.isAdmin = isAdmin;
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getRealName() { return realName; }
    public String getCitizenID() { return citizenID; }
    public double getMoneyOnWallet() { return moneyOnWallet; }
    public boolean getIsBidder() { return isBidder; }
    public boolean getIsSeller() { return isSeller; }
    public boolean getIsAdmin() { return isAdmin; }
    public String getEncodingPassword() { return encodingPassword; }
    

    @Override
    public void displayInfo() {
        System.err.println("User debuging information for developer purpose:");
        System.err.println("ID: " + id);
        System.err.println("Username: " + userName);
        System.err.println("Real name: " + realName);
        System.err.println("Citizen ID: " + citizenID);
        System.err.println("Encoding password: " + encodingPassword);
        System.err.println("Money on wallet: " + moneyOnWallet);
        System.err.println("Created at: " + createdAt);
        System.err.println("Updated at: " + updatedAt);
        System.err.println("Is actived: " + isActived);
        System.err.println("Is bidder: " + isBidder);
        System.err.println("Is seller: " + isSeller);
        System.err.println("Is admin: " + isAdmin);
    }

    @Override
    public boolean isValid() {
        return isActived;
    }

    @Override
    public void updateCitizenID(String citizenID) {
        this.citizenID = citizenID;
        this.update();
    }

}

// class Bidder extends User {

// }