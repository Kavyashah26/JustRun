
"use client"

import type React from "react"

import { useEffect, useState } from "react"
import ReactFlow, {
  Background,
  Controls,
  type Edge,
  type Node,
  Position,
  useEdgesState,
  useNodesState,
  MarkerType,
  ConnectionLineType,
} from "reactflow"
import "reactflow/dist/style.css"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { AlertCircle, CheckCircle, XCircle } from "lucide-react"

interface Task {
  id: string
  name: string
  description: string
  status: string
  chains: Array<{
    id: string
    taskId: string
    statusCode: number
    nextTaskId: string
  }>
}

interface TaskNode extends Node {
  data: {
    label: React.ReactNode
    task: Task
  }
}

export function TaskChainVisualizer({ id }: { id: string }) {
  const [nodes, setNodes, onNodesChange] = useNodesState<TaskNode[]>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [mainTask, setMainTask] = useState<Task | null>(null)
  const [relatedTasks, setRelatedTasks] = useState<Task[]>([])

  // Custom node types
  const nodeTypes = {
    mainTask: MainTaskNode,
    childTask: ChildTaskNode,
  }

  useEffect(() => {
    async function fetchTaskData() {
      setIsLoading(true)
      setError(null)

      try {
        // Fetch the main task
        const response = await fetch(`/api/tasks/${id}`)
        if (!response.ok) {
          throw new Error(`Failed to fetch task: ${response.statusText}`)
        }

        const task = await response.json()
        setMainTask(task)

        // If the task has chains, fetch the related tasks
        if (task.chains && task.chains.length > 0) {
          const relatedTaskIds = task.chains.map((chain: any) => chain.nextTaskId)
          const uniqueTaskIds = [...new Set(relatedTaskIds)]

          const relatedTasksData = await Promise.all(
            uniqueTaskIds.map(async (taskId) => {
              try {
                const taskResponse = await fetch(`/api/tasks/${taskId}`)
                if (!taskResponse.ok) {
                  console.warn(`Could not load task ${taskId}: ${taskResponse.statusText}`)
                  return {
                    id: taskId,
                    name: "Unknown Task",
                    description: "Could not load task details",
                    status: "UNKNOWN",
                    chains: [],
                  }
                }
                return taskResponse.json()
              } catch (err) {
                console.warn(`Error fetching task ${taskId}:`, err)
                return {
                  id: taskId,
                  name: "Unknown Task",
                  description: "Could not load task details",
                  status: "UNKNOWN",
                  chains: [],
                }
              }
            }),
          )

          setRelatedTasks(relatedTasksData)
        }
      } catch (err) {
        console.error("Error fetching task data:", err)
        setError(err instanceof Error ? err.message : "Failed to load task data")
      } finally {
        setIsLoading(false)
      }
    }

    fetchTaskData()
  }, [id])

  useEffect(() => {
    if (!mainTask) return

    // Create flow nodes and edges
    const flowNodes: Node[] = []
    const flowEdges: Edge[] = []

    // Add main task node
    flowNodes.push({
      id: mainTask.id,
      type: "mainTask",
      position: { x: 250, y: 100 },
      data: {
        label: mainTask.name,
        task: mainTask,
      },
      sourcePosition: Position.Bottom,
    })

    // Add related task nodes in a circular pattern
    if (relatedTasks.length > 0) {
      const radius = 250
      const angleStep = (2 * Math.PI) / relatedTasks.length

      relatedTasks.forEach((task, index) => {
        const angle = index * angleStep
        const x = 250 + radius * Math.cos(angle)
        const y = 350 + radius * Math.sin(angle)

        flowNodes.push({
          id: task.id,
          type: "childTask",
          position: { x, y },
          data: {
            label: task.name,
            task,
          },
          targetPosition: Position.Top,
        })
      })

      // Create edges from main task to related tasks
      mainTask.chains.forEach((chain) => {
        flowEdges.push({
          id: `${chain.taskId}-${chain.statusCode}-${chain.nextTaskId}`,
          source: chain.taskId,
          target: chain.nextTaskId,
          label: `Status: ${chain.statusCode}`,
          labelStyle: { fontSize: 12, fill: "var(--foreground)" },
          style: { stroke: "var(--primary)", strokeWidth: 2 },
          animated: true,
          markerEnd: {
            type: MarkerType.ArrowClosed,
            width: 20,
            height: 20,
            color: "var(--primary)",
          },
          type: ConnectionLineType.SmoothStep,
        })
      })
    }

    setNodes(flowNodes)
    setEdges(flowEdges)
  }, [mainTask, relatedTasks, setNodes, setEdges])

  if (isLoading) {
    return (
      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="h-[600px] w-full flex items-center justify-center">
            <div className="space-y-4 w-1/2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-80 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card className="overflow-hidden">
        <CardContent className="p-6">
          <div className="flex items-center justify-center h-[600px]">
            <div className="text-center space-y-4">
              <AlertCircle className="h-12 w-12 text-destructive mx-auto" />
              <h3 className="text-lg font-medium">Failed to load task chain</h3>
              <p className="text-muted-foreground">{error}</p>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (!mainTask || !mainTask.chains || mainTask.chains.length === 0) {
    return (
      <Card className="overflow-hidden">
        <CardContent className="p-6">
          <div className="flex items-center justify-center h-[600px]">
            <div className="text-center space-y-4">
              <XCircle className="h-12 w-12 text-muted-foreground mx-auto" />
              <h3 className="text-lg font-medium">No task chain found</h3>
              <p className="text-muted-foreground">This task does not have any chains configured.</p>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="overflow-hidden">
      <CardContent className="p-0">
        <div className="h-[600px] w-full">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            nodeTypes={nodeTypes}
            fitView
            attributionPosition="bottom-right"
            minZoom={0.5}
            maxZoom={1.5}
            defaultViewport={{ x: 0, y: 0, zoom: 1 }}
          >
            <Background color="var(--muted-foreground)" gap={16} size={1} />
            <Controls />
          </ReactFlow>
        </div>
      </CardContent>
    </Card>
  )
}

// Custom node components
function MainTaskNode({ data }: { data: any }) {
  const task = data.task

  return (
    <div className="p-4 rounded-lg shadow-lg border-2 border-primary bg-card min-w-[200px]">
      <div className="flex items-center gap-2 mb-2">
        <CheckCircle className="h-5 w-5 text-primary" />
        <div className="font-bold text-sm">{task.name}</div>
      </div>
      <div className="text-xs text-muted-foreground mb-2 line-clamp-2">{task.description || "No description"}</div>
      <div className="flex items-center gap-2">
        <Badge variant="outline" className="capitalize">
          {task.status?.toLowerCase() || "Unknown"}
        </Badge>
        <Badge variant="secondary">Root Task</Badge>
      </div>
    </div>
  )
}

function ChildTaskNode({ data }: { data: any }) {
  const task = data.task

  return (
    <div className="p-4 rounded-lg shadow-md border border-border bg-card min-w-[180px]">
      <div className="font-medium text-sm mb-2">{task.name}</div>
      <div className="text-xs text-muted-foreground mb-2 line-clamp-2">{task.description || "No description"}</div>
      <Badge variant="outline" className="capitalize">
        {task.status?.toLowerCase() || "Unknown"}
      </Badge>
    </div>
  )
}
