import { TaskChainVisualizer } from "@/components/tasks/task-chain-visualizer"
import { PageTitle } from "@/components/ui/page-title"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { ArrowLeftIcon } from "lucide-react"

export default function TaskChainPage({ params }: { params: { id: string } }) {
  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <Link href={`/tasks/${params.id}`}>
          <Button variant="outline" size="icon">
            <ArrowLeftIcon className="h-4 w-4" />
          </Button>
        </Link>
        <PageTitle title="Task Chain" description={`Visualization for task: ${params.id}`} />
      </div>
      <TaskChainVisualizer id={params.id} />
    </div>
  )
}
