#!/bin/bash

echo "🚀 Starting Job Tracker..."

# Start infrastructure
echo "📦 Starting Docker services..."
docker compose up -d

# Wait for PostgreSQL health check
echo "⏳ Waiting for PostgreSQL..."

until [ "$(docker inspect -f '{{.State.Health.Status}}' jobtracker-db 2>/dev/null)" = "healthy" ]; do
    echo "   Waiting for PostgreSQL..."
    sleep 2
done

echo "✅ PostgreSQL is healthy!"

# Start backend
echo "☕ Starting Spring Boot backend..."

cd backend || exit 1

./mvnw spring-boot:run &
BACKEND_PID=$!

cd ..

# Wait for backend to be reachable
echo "⏳ Waiting for backend..."

until curl -s http://localhost:8080 >/dev/null 2>&1; do
    sleep 2
done

echo "✅ Backend is running!"

# Start frontend
echo "⚛️  Starting React frontend..."

cd frontend || exit 1

npm start &
FRONTEND_PID=$!

cd ..

echo ""
echo "====================================="
echo "✅ Job Tracker is running!"
echo ""
echo "🌐 Frontend: http://localhost:3000"
echo "🔌 Backend : http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop everything"
echo "====================================="

cleanup() {
    echo ""
    echo "🛑 Shutting down Job Tracker..."

    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null

    docker compose stop

    echo "✅ Shutdown complete"
    exit 0
}

trap cleanup INT TERM

wait