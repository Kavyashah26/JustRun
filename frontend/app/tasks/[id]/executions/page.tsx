
import { PageTitle } from "@/components/ui/page-title"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { ArrowLeftIcon } from "lucide-react"
import { getTask } from "@/lib/task-service"
import { TaskExecutions } from "@/components/tasks/task-executions"

export default async function TaskExecutionsPage({ params }: { params: { id: string } }) {
  // Fetch the task data to get the name
  const task = await getTask(params.id)

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <Link href={`/tasks/${params.id}`}>
          <Button variant="outline" size="icon">
            <ArrowLeftIcon className="h-4 w-4" />
          </Button>
        </Link>
        <PageTitle title="Task Executions" description={`Execution history for: ${task.name}`} />
      </div>
      <TaskExecutions taskId={params.id} />
    </div>
  )
}
