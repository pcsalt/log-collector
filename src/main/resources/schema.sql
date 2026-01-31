CREATE TABLE IF NOT EXISTS logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  service VARCHAR(100) NOT NULL,
  level VARCHAR(10) NOT NULL,
  message TEXT NOT NULL,
  timestamp DATETIME NOT NULL,
  correlation_id VARCHAR(50),
  logger VARCHAR(255),
  thread VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_logs_service ON logs(service);
CREATE INDEX IF NOT EXISTS idx_logs_level ON logs(level);
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_logs_correlation_id ON logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_logs_created_at ON logs(created_at);
