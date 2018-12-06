public class Student {
    private String name;
    private String classes;
    private String address;

    public Student(String name, String classes, String address) {
        this.name = name;
        this.classes = classes;
        this.address = address;
    }

    public Student() {    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
