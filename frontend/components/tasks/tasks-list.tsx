"use client"

import { useState, useEffect, useRef } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useSearchParams } from "next/navigation"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  LinkIcon,
  MoreHorizontalIcon,
  PauseIcon,
  PlayIcon,
  Trash2Icon,
} from "lucide-react"
import { deleteTask, toggleTaskStatus } from "@/lib/task-service"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { toast } from "sonner"

export interface Task {
  id: string
  name: string
  description: string
  endpoint: string
  method: string
  headers: Record<string, string>
  body: any
  cronExpression: string
  priority: "HIGH" | "NORMAL" | "LOW"
  maxRetries: number | null
  retryDelay: number | null
  exponentialBackoff: boolean | null
  webhookUrl: string | null
  status: "ACTIVE" | "PAUSED" | "ERROR"
  createdAt: string
  updatedAt: string
  lastExecutedAt: string | null
  executionCount: number
  failureCount: number
  chains: any[] | null
}

interface TasksListProps {
  initialTasks?: Task[]
}

export function TasksList({ initialTasks = [] }: TasksListProps) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [page, setPage] = useState(1)
  const [tasks, setTasks] = useState<Task[]>(initialTasks)
  const [filteredTasks, setFilteredTasks] = useState<Task[]>(initialTasks)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [taskToDelete, setTaskToDelete] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isTogglingStatus, setIsTogglingStatus] = useState(false)
  const isInitialRender = useRef(true)
  const searchParamsString = useRef("")

  // Apply filters when search params change
  useEffect(() => {
    // Convert searchParams to string to compare with previous value
    const currentSearchParamsString = searchParams.toString()

    // Skip filtering on initial render or if searchParams haven't changed
    if (isInitialRender.current) {
      isInitialRender.current = false
      searchParamsString.current = currentSearchParamsString
      return
    }

    // Skip if searchParams haven't changed
    if (currentSearchParamsString === searchParamsString.current) {
      return
    }

    // Update the ref with current searchParams string
    searchParamsString.current = currentSearchParamsString

    let filtered = [...tasks]

    // Apply search filter
    const searchTerm = searchParams.get("search")
    if (searchTerm) {
      filtered = filtered.filter(
        (task) =>
          task.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          task.endpoint.toLowerCase().includes(searchTerm.toLowerCase()),
      )
    }

    // Apply status filter
    const statusFilter = searchParams.get("status")
    if (statusFilter) {
      const statuses = statusFilter.split(",")
      filtered = filtered.filter((task) => statuses.includes(task.status.toLowerCase()))
    }

    // Apply priority filter
    const priorityFilter = searchParams.get("priority")
    if (priorityFilter) {
      const priorities = priorityFilter.split(",")
      filtered = filtered.filter((task) => priorities.includes(task.priority.toLowerCase()))
    }

    // Apply hasChain filter
    const hasChainFilter = searchParams.get("hasChain")
    if (hasChainFilter === "true") {
      filtered = filtered.filter((task) => task.chains && task.chains.length > 0)
    }

    setFilteredTasks(filtered)
    setPage(1) // Reset to first page when filters change
  }, [searchParams, tasks])

  // Get current page of tasks
  const itemsPerPage = 5
  const currentTasks = filteredTasks.slice((page - 1) * itemsPerPage, page * itemsPerPage)
  const totalPages = Math.ceil(filteredTasks.length / itemsPerPage) || 1

  // Handle task deletion
  const handleDeleteClick = (taskId: string) => {
    setTaskToDelete(taskId)
    setIsDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = async () => {
    if (!taskToDelete) return

    setIsDeleting(true)
    try {
      await deleteTask(taskToDelete)

      // Update the local state to remove the deleted task
      const updatedTasks = tasks.filter((task) => task.id !== taskToDelete)
      setTasks(updatedTasks)
      setFilteredTasks(filteredTasks.filter((task) => task.id !== taskToDelete))

      toast.success("Task deleted", {
        description: "The task has been successfully deleted.",
      })
    } catch (error) {
      console.error("Error deleting task:", error)
      toast.error("Error", {
        description: "Failed to delete the task. Please try again.",
      })
    } finally {
      setIsDeleting(false)
      setIsDeleteDialogOpen(false)
      setTaskToDelete(null)
    }
  }

  // Handle task status toggle (pause/resume)
  const handleToggleStatus = async (taskId: string, currentStatus: string) => {
    setIsTogglingStatus(true)
    try {
      const newStatus = currentStatus === "ACTIVE" ? "PAUSED" : "ACTIVE"
      await toggleTaskStatus(taskId, newStatus as "ACTIVE" | "PAUSED")

      // Update the local state to reflect the status change
      // const updatedTasks = tasks.map((task) => (task.id === taskId ? { ...task, status: newStatus } : task))
      const updatedTasks = tasks.map((task) =>
        task.id === taskId ? { ...task, status: newStatus as Task["status"] } : task
      )
      
      setTasks(updatedTasks)

      toast.success(`Task ${newStatus === "ACTIVE" ? "resumed" : "paused"}`, {
        description: `The task has been ${newStatus === "ACTIVE" ? "resumed" : "paused"} successfully.`,
      })
    } catch (error) {
      console.error("Error toggling task status:", error)
      toast.error("Error", {
        description: "Failed to update the task status. Please try again.",
      })
    } finally {
      setIsTogglingStatus(false)
    }
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Tasks</CardTitle>
          <CardDescription>
            {filteredTasks.length === tasks.length
              ? "Manage your scheduled tasks"
              : `Showing ${filteredTasks.length} of ${tasks.length} tasks`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {filteredTasks.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">No tasks match your filters</p>
              <Button variant="outline" className="mt-4" asChild>
                <Link href="/tasks">Clear filters</Link>
              </Button>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Method</TableHead>
                    <TableHead>Schedule</TableHead>
                    <TableHead>Priority</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Chain</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {currentTasks.map((task) => (
                    <TableRow key={task.id}>
                      <TableCell className="font-medium">
                        <Link href={`/tasks/${task.id}`} className="hover:underline">
                          {task.name}
                        </Link>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{task.method}</Badge>
                      </TableCell>
                      <TableCell className="font-mono text-xs">{task.cronExpression}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            task.priority === "HIGH" ? "default" : task.priority === "LOW" ? "outline" : "secondary"
                          }
                          className="capitalize"
                        >
                          {task.priority.toLowerCase()}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            task.status === "ACTIVE" ? "outline" : task.status === "ERROR" ? "destructive" : "secondary"
                          }
                          className="capitalize"
                        >
                          {task.status.toLowerCase()}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {task.chains && task.chains.length > 0 ? (
                          <LinkIcon className="h-4 w-4 text-muted-foreground" />
                        ) : null}
                      </TableCell>
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon">
                              <MoreHorizontalIcon className="h-4 w-4" />
                              <span className="sr-only">Open menu</span>
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem>
                              <Link href={`/tasks/${task.id}`} className="flex w-full">
                                View details
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem>
                              <Link href={`/tasks/${task.id}/edit`} className="flex w-full">
                                Edit task
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem className="text-destructive" onClick={() => handleDeleteClick(task.id)}>
                              <div className="flex items-center">
                                <Trash2Icon className="mr-2 h-4 w-4" />
                                Delete task
                              </div>
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-end space-x-2 py-4">
                <Button variant="outline" size="sm" onClick={() => setPage(page - 1)} disabled={page === 1}>
                  <ChevronLeftIcon className="h-4 w-4" />
                </Button>
                <Button variant="outline" size="sm" disabled>
                  Page {page} of {totalPages}
                </Button>
                <Button variant="outline" size="sm" onClick={() => setPage(page + 1)} disabled={page === totalPages}>
                  <ChevronRightIcon className="h-4 w-4" />
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this task?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the task and all associated data.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              disabled={isDeleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDeleting ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
