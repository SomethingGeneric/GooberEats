# GooberEats Backend Server
Simple Flask application that actually manages storing the data on behalf of the user(s)

## Usage

### Running with Docker (Recommended)

The easiest way to run the backend is using Docker:

1. Clone repo
2. Copy `config.toml.sample` to `config.toml` and add your API keys
3. Build and run with Docker Compose:
   ```bash
   cd backend
   docker-compose up -d
   ```

The server will be available at `http://localhost:5000` with data persisted in the `cal_data` directory.

To stop the server:
```bash
docker-compose down
```

To rebuild after changes:
```bash
docker-compose up -d --build
```

### Running Manually

* Clone repo
* Install python3/python3-pip/python3-venv depending on distro
  * On debian, `apt install -y python3-venv python3-pip` should do it
* To run attached, just use `./run.sh`
* If you want it managed by systemd, run `sudo ./deploy.sh`
* Now you should have it at `http://127.0.0.1:5000` and any other machine IPv4 (check output for exact IP listening addresses)
* You could/should now do a reverse proxy
* And finally, edit `MainActivity.java` in `../GooberEats` to point to your server
* Profit?

## Features

### AI Calorie Estimation with Caching
The backend includes intelligent calorie estimation with automatic caching to reduce API costs:
- First request for a food item uses AI (OpenAI/Anthropic)
- Subsequent requests for the same item are served from cache
- Cache is stored persistently in SQLite database
- Normalized descriptions (case-insensitive) ensure cache hits

