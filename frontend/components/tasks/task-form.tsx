

"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { TaskChainEditor } from "@/components/tasks/task-chain-editor"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { AlertCircleIcon, PlusIcon, Trash2Icon } from "lucide-react"
import { createTask, updateTask } from "@/lib/task-service"
import { toast } from "sonner"

const formSchema = z
  .object({
    name: z.string().min(2, {
      message: "Name must be at least 2 characters.",
    }),
    description: z.string().optional(),
    endpoint: z.string().url({
      message: "Please enter a valid URL.",
    }),
    method: z.enum(["GET", "POST", "PUT", "DELETE", "PATCH"]),
    cronExpression: z.string().optional(),
    priority: z.enum(["HIGH", "NORMAL", "LOW"]),
    taskType: z.enum(["ROOT", "CHAINED"]),
    headers: z
      .array(
        z.object({
          key: z.string().min(1, "Header key is required"),
          value: z.string().min(1, "Header value is required"),
        }),
      )
      .optional(),
    requestBody: z.string().optional(),
  })
  .refine((data) => data.taskType !== "ROOT" || data.cronExpression, {
    message: "Cron expression is required for root tasks",
    path: ["cronExpression"],
  })

type TaskFormProps = {
  task?: any
  isEditing?: boolean
}

export function TaskForm({ task, isEditing = false }: TaskFormProps) {
  const router = useRouter()
  const [activeTab, setActiveTab] = useState("basic")
  const [chains, setChains] = useState<any[]>(isEditing && task?.chains ? task.chains : [])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [headers, setHeaders] = useState<{ key: string; value: string }[]>(
    isEditing && task?.headers
      ? Object.entries(task.headers).map(([key, value]) => ({
          key,
          value: value as string,
        }))
      : [{ key: "", value: "" }],
  )

  // Initialize form with completely empty values for new tasks
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    mode: "onSubmit", // Only validate when explicitly submitted
    defaultValues: isEditing
      ? {
          name: task?.name || "",
          description: task?.description || "",
          endpoint: task?.endpoint || "",
          method: task?.method || "GET",
          cronExpression: task?.cronExpression || "",
          priority: task?.priority || "NORMAL",
          taskType: task?.taskType || "ROOT",
          requestBody: task?.body || "",
        }
      : {
          // Explicitly set all fields to empty for new tasks
          name: "",
          description: "",
          endpoint: "",
          method: "GET", // Default method is required for the select
          cronExpression: "",
          priority: "NORMAL", // Default priority is required for the select
          taskType: "ROOT", // Default task type
          requestBody: "",
        },
  })

  const addHeader = () => {
    setHeaders([...headers, { key: "", value: "" }])
  }

  const removeHeader = (index: number) => {
    const newHeaders = [...headers]
    newHeaders.splice(index, 1)
    setHeaders(newHeaders)
  }

  const updateHeader = (index: number, field: "key" | "value", value: string) => {
    const newHeaders = [...headers]
    newHeaders[index][field] = value
    setHeaders(newHeaders)
  }

  async function onSubmit(values: z.infer<typeof formSchema>) {
    setIsSubmitting(true)
    setError(null)

    try {
      // Filter out empty headers
      const filteredHeaders = headers.filter((h) => h.key.trim() !== "" && h.value.trim() !== "")

      // Convert headers array to object
      const headersObject = filteredHeaders.reduce(
        (acc, header) => {
          acc[header.key] = header.value
          return acc
        },
        {} as Record<string, string>,
      )
      const headersdata = Object.fromEntries(
        Object.entries(headersObject).map(([key, value]) => [key, String(value)])
      );
      // Prepare the task data
      const taskData = {
        ...values,
        headers: headersdata,
        body: values.requestBody && values.requestBody.trim() !== "" ? JSON.parse(values.requestBody) : null,
        chains: chains.length > 0 ? chains : null,
      }

      // Create or update the task
      if (isEditing && task?.id) {
        await updateTask(task.id, taskData)
        toast.success("Task updated", {
          description: "The task has been updated successfully.",
        })
      } else {
        await createTask(taskData)
        toast.success("Task created", {
          description: "The task has been created successfully.",
        })
      }

      // Redirect to tasks list after successful submission
      router.push("/tasks")
    } catch (err) {
      setIsSubmitting(false)
      const errorMessage = err instanceof Error ? err.message : "An error occurred while saving the task"
      setError(errorMessage)
      toast.error("Error", {
        description: errorMessage,
      })
    }
  }
  const handleTabChange = (newTab: string) => {
    // Always allow tab changes without triggering validation
    setActiveTab(newTab)

    // If moving to the chain tab, make sure we don't auto-submit
    if (newTab === "chain") {
      // Reset any touched fields to prevent auto-validation
      form.clearErrors()
    }
  }

  return (
    <Form {...form}>
      <form
        onSubmit={(e) => {
          // Only submit if explicitly clicking the submit button on the chains tab
          if (activeTab !== "chain") {
            e.preventDefault()
            return false
          }
          return form.handleSubmit(onSubmit)(e)
        }}
        noValidate
      >
        <Card>
          <CardHeader>
            <CardTitle>{isEditing ? "Edit Task" : "Create New Task"}</CardTitle>
            <CardDescription>
              {isEditing
                ? "Update your task configuration and execution chain"
                : "Configure your scheduled task and its execution chain"}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <Alert variant="destructive" className="mb-6">
                <AlertCircleIcon className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Tabs value={activeTab} onValueChange={handleTabChange} className="mt-4">
              <TabsList className="grid w-full grid-cols-4">
                <TabsTrigger value="basic">Basic Info</TabsTrigger>
                <TabsTrigger value="request">Request</TabsTrigger>
                <TabsTrigger value="schedule">Schedule</TabsTrigger>
                <TabsTrigger value="chain">Task Chain</TabsTrigger>
              </TabsList>

              <TabsContent value="basic" className="space-y-4 mt-4">
                <FormField
                  control={form.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Name</FormLabel>
                      <FormControl>
                        <Input placeholder="Task name" {...field} />
                      </FormControl>
                      <FormDescription>A descriptive name for your task.</FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="description"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Description</FormLabel>
                      <FormControl>
                        <Textarea placeholder="Task description" className="resize-none" {...field} />
                      </FormControl>
                      <FormDescription>Optional description of what this task does.</FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="endpoint"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Endpoint URL</FormLabel>
                      <FormControl>
                        <Input placeholder="https://api.example.com" {...field} />
                      </FormControl>
                      <FormDescription>The URL that will be called when the task executes.</FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="method"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>HTTP Method</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select HTTP method" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="GET">GET</SelectItem>
                          <SelectItem value="POST">POST</SelectItem>
                          <SelectItem value="PUT">PUT</SelectItem>
                          <SelectItem value="DELETE">DELETE</SelectItem>
                          <SelectItem value="PATCH">PATCH</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormDescription>The HTTP method to use for the request.</FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="taskType"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Task Type</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select task type" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="ROOT">Root</SelectItem>
                          <SelectItem value="CHAINED">Chained</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormDescription>
                        Root tasks run on a schedule. Chained tasks are triggered by other tasks.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </TabsContent>

              <TabsContent value="request" className="space-y-6 mt-4">
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-medium mb-2">HTTP Headers</h3>
                    <div className="space-y-3">
                      {headers.map((header, index) => (
                        <div key={index} className="flex items-center gap-2">
                          <Input
                            placeholder="Header name"
                            value={header.key}
                            onChange={(e) => updateHeader(index, "key", e.target.value)}
                            className="flex-1"
                          />
                          <Input
                            placeholder="Header value"
                            value={header.value}
                            onChange={(e) => updateHeader(index, "value", e.target.value)}
                            className="flex-1"
                          />
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            onClick={() => removeHeader(index)}
                            disabled={headers.length === 1 && index === 0}
                          >
                            <Trash2Icon className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                    </div>
                    <Button type="button" variant="outline" size="sm" onClick={addHeader} className="mt-2">
                      <PlusIcon className="h-4 w-4 mr-2" />
                      Add Header
                    </Button>
                  </div>

                  <FormField
                    control={form.control}
                    name="requestBody"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Request Body</FormLabel>
                        <FormControl>
                          <Textarea
                            placeholder={`{\n  "key": "value"\n}`}
                            className="font-mono h-32 resize-none"
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>
                          JSON body to send with the request (for POST, PUT, PATCH methods).
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </TabsContent>

              <TabsContent value="schedule" className="space-y-4 mt-4">
                {form.watch("taskType") === "ROOT" ? (
                  <FormField
                    control={form.control}
                    name="cronExpression"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Cron Expression</FormLabel>
                        <FormControl>
                          <Input placeholder="0 * * * *" {...field} />
                        </FormControl>
                        <FormDescription>
                          Schedule using cron syntax (e.g., &quot;0 * * * *&quot; for hourly).
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                ) : (
                  <div className="rounded-md bg-muted p-4">
                    <p className="text-sm text-muted-foreground">
                      Cron expression is not required for chained tasks as they are triggered by other tasks.
                    </p>
                  </div>
                )}
                <FormField
                  control={form.control}
                  name="priority"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Priority</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select priority" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="HIGH">High</SelectItem>
                          <SelectItem value="NORMAL">Normal</SelectItem>
                          <SelectItem value="LOW">Low</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormDescription>
                        Task priority affects execution order when resources are limited.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </TabsContent>

              <TabsContent value="chain" className="space-y-4 mt-4">
                <TaskChainEditor
                  chains={chains}
                  onChange={setChains}
                  currentTaskId={isEditing ? task?.id : undefined}
                />
              </TabsContent>
            </Tabs>
          </CardContent>
          <CardFooter className="flex justify-between">
            <Button variant="outline" type="button" onClick={() => router.back()}>
              Cancel
            </Button>
            <div className="flex space-x-2">
              {activeTab !== "basic" && (
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => {
                    if (activeTab === "request") setActiveTab("basic")
                    if (activeTab === "schedule") setActiveTab("request")
                    if (activeTab === "chain") setActiveTab("schedule")
                  }}
                >
                  Previous
                </Button>
              )}
              <Button
                type="button"
                onClick={() => {
                  // Don't trigger validation when just navigating between tabs
                  if (activeTab === "basic") setActiveTab("request")
                  if (activeTab === "request") setActiveTab("schedule")
                  if (activeTab === "schedule") setActiveTab("chain")
                }}
              >
                Next
              </Button>
              <Button
                type="button"
                disabled={isSubmitting}
                onClick={() => {
                  // Manually trigger form validation and submission
                  form.handleSubmit(onSubmit)()
                }}
              >
                {isSubmitting ? "Saving..." : isEditing ? "Update Task" : "Save Task"}
              </Button>
            </div>
          </CardFooter>
        </Card>
      </form>
    </Form>
  )
}
