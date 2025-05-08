import { TasksFilter } from "@/components/tasks/tasks-filter"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { PlusIcon } from "lucide-react"
import { getServerAuthToken } from "@/lib/auth-utils.server"
import { TasksList } from "@/components/tasks/tasks-list"
import { PageTitle } from "@/components/ui/page-title"

// Server component to fetch tasks
async function getTasks() {
  const token = await getServerAuthToken()
    console.log("token is ",token);
    
  if (!token) {
    return []
  }

  const TASK_SERVICE_URL = process.env.TASK_MANAGEMENT_SERVICE_URL || "http://localhost:8081/"

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      cache: "no-store", // Don't cache this request
    })

    if (!response.ok) {
      console.error("Failed to fetch tasks:", await response.text())
      return []
    }

    return response.json()
  } catch (error) {
    console.error("Error fetching tasks:", error)
    return []
  }
}

export default async function TasksPage() {
  const tasks = await getTasks()

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageTitle title="Tasks" description="Manage your scheduled tasks" />
        <Link href="/tasks/new">
          <Button>
            <PlusIcon className="h-4 w-4 mr-2" />
            New Task
          </Button>
        </Link>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-1">
          <TasksFilter />
        </div>
        <div className="lg:col-span-3">
          <TasksList initialTasks={tasks} />
        </div>
      </div>
    </div>
  )
}
