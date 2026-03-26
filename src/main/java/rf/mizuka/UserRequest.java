package rf.mizuka;

public	class UserRequest {
    public String name;

    public UserRequest() {}

    public String getName() {
        return name;
    }

    public UserRequest setName(String name) {
        this.name = name;
        return this;
    }
}