"use client"

import { TaskChainVisualizer } from "@/components/tasks/task-chain-visualizer"
import { PageTitle } from "@/components/ui/page-title"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { ArrowLeftIcon } from "lucide-react"
import { useParams } from "next/navigation"

export default function TaskChainPageClient() {
  const params = useParams()
  const taskId = params.id as string

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <Link href={`/tasks/${taskId}`}>
          <Button variant="outline" size="icon">
            <ArrowLeftIcon className="h-4 w-4" />
          </Button>
        </Link>
        <PageTitle title="Task Chain" description={`Visualization for task: ${taskId}`} />
      </div>
      <TaskChainVisualizer id={taskId} />
    </div>
  )
}
