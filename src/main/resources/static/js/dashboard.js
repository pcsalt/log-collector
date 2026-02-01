const LogCollector = {
  stompClient: null,
  connected: false,
  liveTail: true,
  logs: [],
  maxLogs: 1000,
  filters: {
    service: '',
    levels: ['DEBUG', 'INFO', 'WARN', 'ERROR'],
    search: '',
    correlationId: ''
  },

  init() {
    this.bindEvents();
    this.connect();
    this.loadInitialLogs();
  },

  bindEvents() {
    document.getElementById('serviceFilter').addEventListener('change', (e) => {
      this.filters.service = e.target.value;
      this.applyFilters();
    });

    document.querySelectorAll('.level-checkbox input').forEach(checkbox => {
      checkbox.addEventListener('change', () => {
        this.filters.levels = Array.from(document.querySelectorAll('.level-checkbox input:checked'))
          .map(cb => cb.value);
        this.applyFilters();
      });
    });

    let searchTimeout;
    document.getElementById('searchInput').addEventListener('input', (e) => {
      clearTimeout(searchTimeout);
      searchTimeout = setTimeout(() => {
        this.filters.search = e.target.value.toLowerCase();
        this.applyFilters();
      }, 300);
    });

    let correlationTimeout;
    document.getElementById('correlationInput').addEventListener('input', (e) => {
      clearTimeout(correlationTimeout);
      correlationTimeout = setTimeout(() => {
        this.filters.correlationId = e.target.value;
        this.applyFilters();
      }, 300);
    });

    document.getElementById('liveTailBtn').addEventListener('click', () => {
      this.liveTail = !this.liveTail;
      document.getElementById('liveTailBtn').classList.toggle('active', this.liveTail);
      if (this.liveTail) {
        this.scrollToBottom();
      }
    });

    document.getElementById('refreshBtn').addEventListener('click', () => {
      this.loadInitialLogs();
    });

    document.getElementById('clearBtn').addEventListener('click', () => {
      this.logs = [];
      this.renderLogs();
    });
  },

  connect() {
    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;

    this.stompClient.connect({},
      () => {
        this.connected = true;
        this.updateConnectionStatus();
        this.subscribe();
      },
      (error) => {
        this.connected = false;
        this.updateConnectionStatus();
        console.error('WebSocket error:', error);
        setTimeout(() => this.connect(), 5000);
      }
    );
  },

  subscribe() {
    this.stompClient.subscribe('/topic/logs', (message) => {
      const log = JSON.parse(message.body);
      this.addLog(log);
    });
  },

  updateConnectionStatus() {
    const dot = document.getElementById('statusDot');
    const text = document.getElementById('statusText');

    if (this.connected) {
      dot.classList.add('connected');
      text.textContent = 'Connected';
    } else {
      dot.classList.remove('connected');
      text.textContent = 'Disconnected';
    }
  },

  async loadInitialLogs() {
    try {
      const params = new URLSearchParams({ limit: '500' });

      // Add filter parameters
      if (this.filters.service) {
        params.append('service', this.filters.service);
      }

      if (this.filters.levels && this.filters.levels.length > 0) {
        this.filters.levels.forEach(level => {
          params.append('levels', level);
        });
      }

      if (this.filters.search) {
        params.append('search', this.filters.search);
      }

      if (this.filters.correlationId) {
        params.append('correlationId', this.filters.correlationId);
      }

      const response = await fetch(`/api/logs?${params}`);
      const data = await response.json();

      this.logs = data.logs.reverse();
      this.renderLogs();
      this.scrollToBottom();
    } catch (error) {
      console.error('Failed to load logs:', error);
    }
  },

  addLog(log) {
    this.logs.push(log);

    if (this.logs.length > this.maxLogs) {
      this.logs.shift();
    }

    if (this.matchesFilters(log)) {
      this.appendLogRow(log);
      if (this.liveTail) {
        this.scrollToBottom();
      }
    }

    this.updateLogCount();
    this.updateLastUpdate();
  },

  matchesFilters(log) {
    if (this.filters.service && log.service !== this.filters.service) {
      return false;
    }

    if (!this.filters.levels.includes(log.level)) {
      return false;
    }

    if (this.filters.search && !log.message.toLowerCase().includes(this.filters.search)) {
      return false;
    }

    if (this.filters.correlationId && log.correlationId !== this.filters.correlationId) {
      return false;
    }

    return true;
  },

  applyFilters() {
    // Reload logs from backend with filters
    this.loadInitialLogs();
  },

  renderLogs() {
    const tbody = document.getElementById('logTableBody');
    tbody.innerHTML = '';

    const filteredLogs = this.logs.filter(log => this.matchesFilters(log));

    filteredLogs.forEach(log => {
      this.appendLogRow(log, false);
    });

    this.updateLogCount();

    if (this.liveTail) {
      this.scrollToBottom();
    }
  },

  appendLogRow(log, scroll = true) {
    const tbody = document.getElementById('logTableBody');
    const row = document.createElement('tr');
    row.className = 'log-row';
    row.dataset.logId = log.id;

    const timestamp = new Date(log.timestamp).toLocaleString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });

    const correlationBadge = log.correlationId
      ? `<span class="correlation-id">[${log.correlationId}]</span>`
      : '';

    row.innerHTML = `
      <td class="timestamp">${timestamp}</td>
      <td class="service">${this.escapeHtml(log.service)}</td>
      <td><span class="level ${log.level.toLowerCase()}">${log.level}</span></td>
      <td class="message">${this.escapeHtml(log.message)}${correlationBadge}</td>
    `;

    row.addEventListener('click', () => this.toggleLogDetails(row, log));

    tbody.appendChild(row);
  },

  toggleLogDetails(row, log) {
    const existingDetails = row.nextElementSibling;

    if (existingDetails && existingDetails.classList.contains('log-details')) {
      existingDetails.remove();
      row.classList.remove('expanded');
      return;
    }

    document.querySelectorAll('.log-details').forEach(el => el.remove());
    document.querySelectorAll('.log-row.expanded').forEach(el => el.classList.remove('expanded'));

    row.classList.add('expanded');

    const detailsRow = document.createElement('tr');
    detailsRow.innerHTML = `
      <td colspan="4">
        <div class="log-details visible">
          <div class="log-details-grid">
            <span class="log-details-label">ID:</span>
            <span class="log-details-value">${log.id}</span>

            <span class="log-details-label">Service:</span>
            <span class="log-details-value">${this.escapeHtml(log.service)}</span>

            <span class="log-details-label">Level:</span>
            <span class="log-details-value">${log.level}</span>

            <span class="log-details-label">Timestamp:</span>
            <span class="log-details-value">${log.timestamp}</span>

            <span class="log-details-label">Logger:</span>
            <span class="log-details-value">${this.escapeHtml(log.logger || '-')}</span>

            <span class="log-details-label">Thread:</span>
            <span class="log-details-value">${this.escapeHtml(log.thread || '-')}</span>

            <span class="log-details-label">Correlation:</span>
            <span class="log-details-value">${this.escapeHtml(log.correlationId || '-')}</span>

            <span class="log-details-label">Message:</span>
            <span class="log-details-value">${this.escapeHtml(log.message)}</span>
          </div>
        </div>
      </td>
    `;
    detailsRow.classList.add('log-details');

    row.after(detailsRow);
  },

  scrollToBottom() {
    const wrapper = document.getElementById('logTableWrapper');
    wrapper.scrollTop = wrapper.scrollHeight;
  },

  updateLogCount() {
    const filteredCount = this.logs.filter(log => this.matchesFilters(log)).length;
    document.getElementById('logCount').textContent = `${filteredCount} logs (${this.logs.length} total)`;
  },

  updateLastUpdate() {
    const now = new Date().toLocaleTimeString();
    document.getElementById('lastUpdate').textContent = `Last update: ${now}`;
  },

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
};

document.addEventListener('DOMContentLoaded', () => {
  LogCollector.init();
});
