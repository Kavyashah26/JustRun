"use client"

import { useState, useEffect, useRef } from "react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { PlusIcon, Trash2Icon, AlertCircleIcon, LoaderIcon } from "lucide-react"
import { Alert, AlertDescription } from "@/components/ui/alert"

// Common HTTP status codes
const HTTP_STATUS_CODES = [
  { value: "200", label: "200 - OK" },
  { value: "201", label: "201 - Created" },
  { value: "202", label: "202 - Accepted" },
  { value: "204", label: "204 - No Content" },
  { value: "400", label: "400 - Bad Request" },
  { value: "401", label: "401 - Unauthorized" },
  { value: "403", label: "403 - Forbidden" },
  { value: "404", label: "404 - Not Found" },
  { value: "409", label: "409 - Conflict" },
  { value: "422", label: "422 - Unprocessable Entity" },
  { value: "429", label: "429 - Too Many Requests" },
  { value: "500", label: "500 - Internal Server Error" },
  { value: "502", label: "502 - Bad Gateway" },
  { value: "503", label: "503 - Service Unavailable" },
  { value: "504", label: "504 - Gateway Timeout" },
]

// Valid HTTP status code range
const MIN_STATUS_CODE = 100
const MAX_STATUS_CODE = 599

interface TaskChainEditorProps {
  chains: any[]
  onChange: (chains: any[]) => void
  currentTaskId?: string // To exclude current task from available tasks
}

export function TaskChainEditor({ chains, onChange, currentTaskId }: TaskChainEditorProps) {
  const [statusCode, setStatusCode] = useState("")
  const [customStatusCode, setCustomStatusCode] = useState("")
  const [nextTaskId, setNextTaskId] = useState("")
  const [availableTasks, setAvailableTasks] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [useCustomCode, setUseCustomCode] = useState(false)
  const initialized = useRef(false)

  // Fetch available tasks from the backend
  useEffect(() => {
    if (!initialized.current) {
      setIsLoading(true)
      setError(null)

      // Fetch tasks from the backend
      fetch("/api/tasks/list")
        .then((response) => {
          if (!response.ok) {
            throw new Error("Failed to fetch tasks")
          }
          return response.json()
        })
        .then((data) => {
          // Filter out the current task if provided
          const filteredTasks = currentTaskId ? data.filter((task: any) => task.id !== currentTaskId) : data

          setAvailableTasks(filteredTasks)
          setIsLoading(false)
          initialized.current = true
        })
        .catch((err) => {
          console.error("Error fetching tasks:", err)
          setError("Failed to load available tasks. Please try again.")
          setIsLoading(false)
          initialized.current = true
        })
    }
  }, [currentTaskId])

  const addChain = () => {
    let finalStatusCode: number

    if (useCustomCode) {
      if (!customStatusCode) {
        setError("Please enter a status code")
        return
      }

      const code = Number.parseInt(customStatusCode, 10)
      if (isNaN(code) || code < MIN_STATUS_CODE || code > MAX_STATUS_CODE) {
        setError(`Status code must be a number between ${MIN_STATUS_CODE} and ${MAX_STATUS_CODE}`)
        return
      }

      finalStatusCode = code
    } else {
      if (!statusCode) {
        setError("Please select a status code")
        return
      }
      finalStatusCode = Number.parseInt(statusCode, 10)
    }

    if (!nextTaskId) {
      setError("Please select a next task")
      return
    }

    // Check if this status code is already in the chain
    const existingChain = chains.find((chain) => chain.statusCode === finalStatusCode)
    if (existingChain) {
      setError(`Status code ${finalStatusCode} is already configured in the chain`)
      return
    }

    // Find the task name for display purposes
    const selectedTask = availableTasks.find((task) => task.id === nextTaskId)
    const nextTaskName = selectedTask ? selectedTask.name : "Unknown Task"

    setError(null)
    const newChain = {
      statusCode: finalStatusCode,
      nextTaskId,
      nextTaskName, // Store the name for display purposes
    }
    onChange([...chains, newChain])
    setStatusCode("")
    setCustomStatusCode("")
    setNextTaskId("")
    setUseCustomCode(false)
  }

  const removeChain = (index: number) => {
    const newChains = [...chains]
    newChains.splice(index, 1)
    onChange(newChains)
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Task Chain Configuration</CardTitle>
          <CardDescription>Define what happens after this task executes based on HTTP status codes</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertCircleIcon className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <div className="flex flex-col space-y-4 md:flex-row md:space-x-4 md:space-y-0">
            <div className="flex-1 space-y-2">
              <label htmlFor="statusCode" className="text-sm font-medium">
                HTTP Status Code
              </label>
              {useCustomCode ? (
                <div className="flex space-x-2">
                  <Input
                    id="customStatusCode"
                    type="number"
                    min={MIN_STATUS_CODE}
                    max={MAX_STATUS_CODE}
                    placeholder="Enter status code"
                    value={customStatusCode}
                    onChange={(e) => setCustomStatusCode(e.target.value)}
                  />
                  <Button type="button" variant="outline" onClick={() => setUseCustomCode(false)}>
                    Use Preset
                  </Button>
                </div>
              ) : (
                <div className="flex space-x-2">
                  <Select value={statusCode} onValueChange={setStatusCode}>
                    <SelectTrigger id="statusCode">
                      <SelectValue placeholder="Select status code" />
                    </SelectTrigger>
                    <SelectContent>
                      {HTTP_STATUS_CODES.map((code) => (
                        <SelectItem key={code.value} value={code.value}>
                          {code.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button type="button" variant="outline" onClick={() => setUseCustomCode(true)}>
                    Custom
                  </Button>
                </div>
              )}
            </div>
            <div className="flex-1 space-y-2">
              <label htmlFor="nextTask" className="text-sm font-medium">
                Next Task
              </label>
              <Select value={nextTaskId} onValueChange={setNextTaskId} disabled={isLoading}>
                <SelectTrigger id="nextTask">
                  <SelectValue placeholder={isLoading ? "Loading tasks..." : "Select task"} />
                </SelectTrigger>
                <SelectContent>
                  {isLoading ? (
                    <div className="flex items-center justify-center p-2">
                      <LoaderIcon className="h-4 w-4 animate-spin mr-2" />
                      <span>Loading tasks...</span>
                    </div>
                  ) : availableTasks.length === 0 ? (
                    <div className="p-2 text-center text-sm text-muted-foreground">No tasks available</div>
                  ) : (
                    availableTasks.map((task) => (
                      <SelectItem key={task.id} value={task.id}>
                        {task.name}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="flex items-end">
              <Button type="button" onClick={addChain} disabled={isLoading}>
                <PlusIcon className="h-4 w-4 mr-2" />
                Add
              </Button>
            </div>
          </div>

          <div className="space-y-2">
            <h3 className="text-sm font-medium">Configured Chains</h3>
            {chains.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No chains configured yet. Add a chain to define what happens after this task executes.
              </p>
            ) : (
              <div className="space-y-2">
                {chains.map((chain, index) => {
                  // Try to find the task name in available tasks if not already in the chain
                  let taskName = chain.nextTaskName
                  if (!taskName) {
                    const task = availableTasks.find((task) => task.id === chain.nextTaskId)
                    taskName = task ? task.name : "Unknown Task"
                  }

                  const statusCodeLabel =
                    HTTP_STATUS_CODES.find((code) => Number(code.value) === chain.statusCode)?.label ||
                    `${chain.statusCode}`

                  return (
                    <div key={index} className="flex items-center justify-between rounded-md border border-border p-3">
                      <div className="flex items-center space-x-3">
                        <Badge variant="outline">{statusCodeLabel}</Badge>
                        <span className="text-sm">→ {taskName}</span>
                      </div>
                      <Button variant="ghost" size="icon" onClick={() => removeChain(index)}>
                        <Trash2Icon className="h-4 w-4" />
                      </Button>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Chain Visualization</CardTitle>
          <CardDescription>Visual representation of your task chain</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] w-full border border-border rounded-md p-4 flex items-center justify-center">
            {chains.length === 0 ? (
              <div className="text-center text-muted-foreground">
                <p>No chain configured yet</p>
                <p className="text-sm">Add status codes and next tasks above to visualize the chain</p>
              </div>
            ) : (
              <div className="w-full h-full relative flex items-center justify-center">
                {/* Current task node */}
                <div className="absolute left-1/4 transform -translate-x-1/2 w-32 h-24 rounded-md border-2 border-primary bg-background flex items-center justify-center z-10">
                  <div className="text-center text-xs p-2">
                    <div className="font-medium">Current Task</div>
                    <div className="text-muted-foreground mt-1">Starting point</div>
                  </div>
                </div>

                {chains.map((chain, index) => {
                  // Try to find the task name in available tasks if not already in the chain
                  let taskName = chain.nextTaskName
                  if (!taskName) {
                    const task = availableTasks.find((task) => task.id === chain.nextTaskId)
                    taskName = task ? task.name : "Unknown Task"
                  }

                  const yPosition = 50 + (index * 150) / (chains.length || 1)

                  return (
                    <div
                      key={index}
                      className="absolute"
                      style={{ top: `${yPosition}%`, right: "25%", transform: "translateX(50%)" }}
                    >
                      {/* Arrow */}
                      <svg
                        className="absolute top-1/2 right-full -translate-y-1/2 mr-4"
                        width="100"
                        height="2"
                        viewBox="0 0 100 2"
                      >
                        <line
                          x1="0"
                          y1="1"
                          x2="100"
                          y2="1"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeDasharray="4 2"
                        />
                      </svg>

                      {/* Status code label */}
                      <div className="absolute top-1/2 right-full transform -translate-y-1/2 -translate-x-12 text-xs px-2 py-1 bg-muted rounded">
                        Status: {chain.statusCode}
                      </div>

                      {/* Next task node */}
                      <div className="w-32 h-24 rounded-md border border-border bg-background flex items-center justify-center shadow-sm">
                        <div className="text-center text-xs p-2">
                          <div className="font-medium truncate max-w-[120px]">{taskName}</div>
                          <div className="text-muted-foreground mt-1 truncate max-w-[120px]">Next task</div>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
