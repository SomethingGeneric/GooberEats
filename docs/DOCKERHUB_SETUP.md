# DockerHub Integration Setup

This document explains how to set up the GitHub Actions workflow to automatically build and push Docker images to DockerHub.

## Prerequisites

1. A DockerHub account (free at https://hub.docker.com/)
2. Repository admin access to configure GitHub secrets

## Setup Instructions

### Step 1: Create a DockerHub Access Token

1. Log in to [DockerHub](https://hub.docker.com/)
2. Click on your username in the top-right corner
3. Select **Account Settings** → **Security**
4. Click **New Access Token**
5. Give it a description (e.g., "GooberEats GitHub Actions")
6. Set access permissions to **Read, Write, Delete**
7. Click **Generate**
8. **Copy the token** - you won't be able to see it again!

### Step 2: Add Secrets to GitHub Repository

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add the following two secrets:

   **DOCKERHUB_USERNAME**
   - Name: `DOCKERHUB_USERNAME`
   - Value: `verygeneric` (the DockerHub username for this project)

   **DOCKERHUB_TOKEN**
   - Name: `DOCKERHUB_TOKEN`
   - Value: The access token you generated in Step 1

### Step 3: Verify Setup

The workflow will automatically trigger when:
- Code is pushed to the `main` branch with changes in the `backend/` directory
- A pull request is opened targeting `main` with backend changes
- Manually triggered via the "Actions" tab

**Note**: The workflow will **only push to DockerHub** when:
- Triggered by push to `main` branch, OR
- Manually triggered via `workflow_dispatch`

Pull requests will **only build** the Docker image to verify it builds successfully, but will **not push** to DockerHub.

To manually test:
1. Go to the **Actions** tab in your GitHub repository
2. Select **Build and Push Backend Docker Image** workflow
3. Click **Run workflow**
4. Select the branch and click **Run workflow**

### Step 4: Using the Docker Image

Once the workflow runs successfully, your image will be available on DockerHub at `verygeneric/goobereats`:

```bash
# Pull the latest image
docker pull verygeneric/goobereats:latest

# Or pull a specific dated version
docker pull verygeneric/goobereats:2026.02.17.42

# Run the container
docker run -d \
  -p 5000:5000 \
  -v $(pwd)/cal_data:/app/cal_data \
  -v $(pwd)/config.toml:/app/config.toml:ro \
  verygeneric/goobereats:latest
```

Or use docker-compose by updating the image name in `docker-compose.yml`:

```yaml
services:
  goobereats-backend:
    image: verygeneric/goobereats:latest
    # Or use a specific version:
    # image: verygeneric/goobereats:2026.02.17.42
    # ... rest of configuration
```

## Workflow Details

The workflow:
- Builds the Docker image from the `backend/` directory for all triggers
- Tags images with date-based versioning: `YYYY.MM.DD.R`
  - Example: `2026.02.17.42` (where 42 is the workflow run number)
  - `latest` tag (only for main branch pushes)
  - `pr-XX` tag (only for pull requests)
- **Pushes to DockerHub only when**:
  - Push to `main` branch, OR
  - Manually triggered via `workflow_dispatch`
- **Pull requests**: Build only (no push) to verify Docker build succeeds
- Uses layer caching to speed up builds
- Only runs when backend code changes

The date-based tagging ensures:
- Clear versioning with timestamp information
- Unique tags for each build via run number
- Easy identification of when an image was built
- No conflicts with Docker tag naming restrictions

## Troubleshooting

### "Error: Cannot authenticate to Docker Hub"
- Verify your DockerHub username and token are correct
- Ensure the token hasn't expired
- Check that secrets are named exactly `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`

### "Error: Repository not found"
- The repository will be created automatically on first push
- Ensure your DockerHub username in the secrets is correct

### Build fails
- Check the Actions tab for detailed error logs
- Verify the Dockerfile builds locally: `cd backend && docker build -t test .`

## Security Notes

- Never commit your DockerHub token to the repository
- Rotate your access tokens periodically
- Use read-only tokens if you only need to pull images
- The workflow only pushes on main branch merges, not on PRs
