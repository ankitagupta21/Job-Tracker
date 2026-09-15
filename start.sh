#!/bin/bash

echo "🚀 Starting Job Tracker..."

mkdir -p "$HOME/.job-tracker"

echo "📦 Building and starting all services..."
docker compose up --build -d

echo "⏳ Waiting for PostgreSQL..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' jobtracker-db 2>/dev/null)" = "healthy" ]; do
    echo "   Waiting for PostgreSQL..."
    sleep 2
done
echo "✅ PostgreSQL is healthy!"

echo "⏳ Waiting for backend..."
until curl -s http://localhost:8080/api/gmail/status >/dev/null 2>&1; do
    sleep 2
done
echo "✅ Backend is running!"

echo ""
echo "====================================="
echo "✅ Job Tracker is running!"
echo ""
echo "🌐 Frontend: http://localhost:3000"
echo "🔌 Backend : http://localhost:8080"
echo "📧 Connect Gmail: http://localhost:8080/auth/gmail"
echo ""
echo "Run 'docker compose logs -f' to follow logs."
echo "Run 'docker compose down' to stop everything."
echo "====================================="
