
import { TaskDetails } from "@/components/tasks/task-details"
import { PageTitle } from "@/components/ui/page-title"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { ArrowLeftIcon, PencilIcon } from "lucide-react"
import { getTask } from "@/lib/task-service"

export default async function TaskPage({ params }: { params: { id: string } }) {
  // Await params before accessing its properties
  const { id } = await params
  // Fetch the task data from the API
  const task = await getTask(id)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link href="/tasks">
            <Button variant="outline" size="icon">
              <ArrowLeftIcon className="h-4 w-4" />
            </Button>
          </Link>
          <PageTitle title="Task Details" description={`Task: ${task.name}`} />
        </div>
        <Link href={`/tasks/${id}/edit`}>
          <Button variant="outline">
            <PencilIcon className="h-4 w-4 mr-2" />
            Edit
          </Button>
        </Link>
      </div>
      <TaskDetails task={task} />
    </div>
  )
}
