# GooberEats Backend Server
Simple Flask application that actually manages storing the data on behalf of the user(s)

## Usage
* Clone repo
* Install python3/python3-pip/python3-venv depending on distro
  * On debian, `apt install -y python3-venv python3-pip` should do it
* To run attached, just use `./run.sh`
* If you want it managed by systemd, run `sudo ./deploy.sh`
* Now you should have it at `http://127.0.0.1:5000` and any other machine IPv4 (check output for exact IP listening addresses)
* You could/should now do a reverse proxy
* And finally, edit `MainActivity.java` in `../GooberEats` to point to your server
* Profit?
