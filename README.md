# JustRun

JustRun is a **cloud-native platform** for scheduling and automating HTTP-based tasks (API calls) without the need to manage any persistent infrastructure. It's purpose-built for **serverless** and **microservice** architectures where traditional cron jobs and background workers are infeasible.

---

## 🚀 Key Features

- **No Infrastructure Management**: Run scheduled tasks without needing servers.
- **Support for Workflows**: Chain tasks together with conditional logic.
- **Priority Queues**: Handle critical, normal, and low-priority tasks separately.
- **Resilient & Scalable**: Backed by AWS services for high reliability.
- **Retry & Backoff**: Automatic retries with exponential backoff for failed tasks.
- **Detailed Execution Logs**: Full audit trail of each task execution.

---

## 🧩 Core Components

| Service | Description |
|--------|-------------|
| **Auth Service** | Manages user authentication, registration, and API keys. |
| **Task Manager** | Handles creation, updating, and deletion of tasks. |
| **Cron Scanner** | Periodically scans for tasks due for execution and claims them. |
| **Task Executor** | Executes HTTP requests, handles retries, and manages chained tasks. |

---

## 🔄 Task Types

- **ROOT Task**: Scheduled using a cron expression (e.g., `0 9 * * *`).
- **CHAINED Task**: Executes only upon completion of a parent task, based on success/failure/condition.

---

## 📈 Task Execution Flow

1. **Create** a ROOT task via API with a schedule.
2. **Cron Scanner** detects and claims the due task.
3. Task is placed in **priority queue**.
4. **Task Executor** runs the task and logs the result.
5. If **chained tasks** exist, they're evaluated and executed accordingly.

---

## 📦 AWS Integration

- **DynamoDB**: Task metadata, execution history, user data.
- **SQS**: Priority queues (`HIGH`, `NORMAL`, `LOW`).
- **CloudWatch**: Logging and system metrics.

| Priority | Scan Frequency |
|----------|----------------|
| High     | Every 5 sec     |
| Normal   | Every 10 sec    |
| Low      | Every 20 sec    |

---

## 🔒 Reliability & Resilience

- **Concurrency Control**: Prevents duplicate execution in distributed environments.
- **Exponential Backoff**: For retrying failed tasks.
- **Execution Logs**: For auditing and debugging.

---

## 💼 Real-World Use Cases

- **Daily Reports**: Trigger automated report generation via APIs.
- **User Onboarding**: Execute step-by-step onboarding flows.
- **Data Sync**: Regularly sync multiple systems through API calls.
- **Multi-step Workflows**: With branching logic based on responses.

---

## ✅ Benefits Over Traditional Cron Jobs

- No persistent compute resources required.
- Easily scale to thousands of tasks.
- Full observability with retry logic.
---

## 🧠 Maintainers

Built and maintained with ❤️ by the Kavya.

---
