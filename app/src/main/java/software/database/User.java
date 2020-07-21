package software.database;

public class User {
 int id;
 String name,username,password, MobNumber1;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMobNumber1() {
		return MobNumber1;
	}

	public void setMobNumber1(String mobNumber1) {
		MobNumber1 = mobNumber1;
	}



	public User(int id, String name, String username, String password, String mobNumber1, String mobNumber2) {
		this.id = id;
		this.name = name;
		this.username = username;
		this.password = password;
		MobNumber1 = mobNumber1;
	}

	public User() {
	}
}
