import { TaskForm } from "@/components/tasks/task-form"
import { PageTitle } from "@/components/ui/page-title"

export default function NewTaskPage() {
  return (
    <div className="space-y-6">
      <PageTitle title="Create Task" description="Create a new scheduled task" />
      {/* Passing an empty task object and explicitly set isEditing to false */}
      <TaskForm task={{}} isEditing={false} />
    </div>
  )
}
