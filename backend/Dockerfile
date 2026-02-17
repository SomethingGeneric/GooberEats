# Use Python 3.11 slim image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy requirements file
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY *.py .
COPY templates/ templates/

# Create volume mount point for persistent data
VOLUME /app/cal_data

# Expose Flask default port
EXPOSE 5000

# Set environment variable for Flask
ENV FLASK_APP=server.py

# Run the application
CMD ["python", "server.py"]
