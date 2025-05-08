
import Link from "next/link"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { LinkIcon } from "lucide-react"
import { TaskExecutionsList } from "./task-execution-list"
import { ReactElement, JSXElementConstructor, ReactNode, ReactPortal, Key } from "react"

// Mock recent executions - in a real app, you would fetch these from an API
const recentExecutions = [
  {
    id: "exec_1",
    status: "success",
    timestamp: "2023-05-05T08:30:00Z",
    duration: "45s",
    statusCode: 200,
  },
  {
    id: "exec_2",
    status: "success",
    timestamp: "2023-05-04T08:30:00Z",
    duration: "43s",
    statusCode: 200,
  },
  {
    id: "exec_3",
    status: "failed",
    timestamp: "2023-05-03T08:30:00Z",
    duration: "12s",
    statusCode: 500,
  },
  {
    id: "exec_4",
    status: "success",
    timestamp: "2023-05-02T08:30:00Z",
    duration: "44s",
    statusCode: 200,
  },
]

interface TaskDetailsProps {
  task: any
}

export function TaskDetails({ task }: TaskDetailsProps) {
  // Format the headers for display
  const headers = task.headers ? Object.entries(task.headers).map(([key, value]) => ({ key, value })) : []

  return (
    <div className="space-y-6">
      <Tabs defaultValue="overview">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="executions">Executions</TabsTrigger>
          <TabsTrigger value="configuration">Configuration</TabsTrigger>
          <TabsTrigger value="chain">Task Chain</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Task Information</CardTitle>
                <CardDescription>Basic information about this task</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm font-medium">Status</p>
                    <Badge variant={task.status === "ACTIVE" ? "outline" : "secondary"} className="mt-1 capitalize">
                      {task.status.toLowerCase()}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Priority</p>
                    <Badge
                      variant={task.priority === "HIGH" ? "default" : task.priority === "LOW" ? "outline" : "secondary"}
                      className="mt-1 capitalize"
                    >
                      {task.priority.toLowerCase()}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Method</p>
                    <Badge variant="outline" className="mt-1">
                      {task.method}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Has Chain</p>
                    <div className="mt-1">
                      {task.chains && task.chains.length > 0 ? (
                        <div className="flex items-center">
                          <LinkIcon className="h-4 w-4 mr-1 text-muted-foreground" />
                          <span className="text-sm">Yes</span>
                        </div>
                      ) : (
                        <span className="text-sm">No</span>
                      )}
                    </div>
                  </div>
                </div>

                <div>
                  <p className="text-sm font-medium">Description</p>
                  <p className="text-sm text-muted-foreground mt-1">{task.description || "No description provided"}</p>
                </div>

                <div>
                  <p className="text-sm font-medium">Endpoint</p>
                  <p className="text-sm font-mono mt-1 truncate">{task.endpoint}</p>
                </div>

                <div>
                  <p className="text-sm font-medium">Schedule</p>
                  <p className="text-sm font-mono mt-1">{task.cronExpression}</p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Execution Statistics</CardTitle>
                <CardDescription>Performance metrics for this task</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm font-medium">Last Execution</p>
                    <p className="text-sm text-muted-foreground mt-1">
                      {task.lastExecutedAt ? new Date(task.lastExecutedAt).toLocaleString() : "Never executed"}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Next Execution</p>
                    <p className="text-sm text-muted-foreground mt-1">
                      {task.nextExecutionTime ? new Date(task.nextExecutionTime).toLocaleString() : "Not scheduled"}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Success Rate</p>
                    <p className="text-sm text-muted-foreground mt-1">
                      {task.executionCount > 0
                        ? Math.round(((task.executionCount - task.failureCount) / task.executionCount) * 100)
                        : 0}
                      %
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Avg. Duration</p>
                    <p className="text-sm text-muted-foreground mt-1">N/A</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Success Count</p>
                    <p className="text-sm text-muted-foreground mt-1">{task.executionCount - task.failureCount}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium">Failure Count</p>
                    <p className="text-sm text-muted-foreground mt-1">{task.failureCount}</p>
                  </div>
                </div>
              </CardContent>
              <CardFooter>
                <Button variant="outline" className="w-full" asChild>
                  <Link href={`/tasks/${task.id}/executions`}>View All Executions</Link>
                </Button>
              </CardFooter>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Recent Executions</CardTitle>
              <CardDescription>Latest execution results</CardDescription>
            </CardHeader>
            <CardContent>
              {task.executionCount === 0 ? (
                <p className="text-sm text-muted-foreground">No executions yet</p>
              ) : (
                <div className="space-y-4">
                  {recentExecutions.map((execution) => (
                    <div
                      key={execution.id}
                      className="flex items-center justify-between border-b border-border pb-4 last:border-0 last:pb-0"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <Badge
                            variant={execution.status === "success" ? "outline" : "destructive"}
                            className="capitalize"
                          >
                            {execution.status}
                          </Badge>
                          <span className="text-sm">Status Code: {execution.statusCode}</span>
                        </div>
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <span>{new Date(execution.timestamp).toLocaleString()}</span>
                          <span>•</span>
                          <span>{execution.duration}</span>
                        </div>
                      </div>
                      <Link href={`/executions/${execution.id}`}>
                        <Button variant="ghost" size="sm">
                          View
                        </Button>
                      </Link>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="executions">
          <Card>
            <CardHeader>
              <CardTitle>Execution History</CardTitle>
              <CardDescription>Complete history of task executions</CardDescription>
            </CardHeader>
            <CardContent>
              <TaskExecutionsList taskId={task.id} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="configuration">
          <Card>
            <CardHeader>
              <CardTitle>Task Configuration</CardTitle>
              <CardDescription>Technical configuration details</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h3 className="text-sm font-medium">Cron Expression</h3>
                <p className="text-sm font-mono mt-1">{task.cronExpression}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {task.cronExpression === "0 * * * * ?" ? "Runs every minute" : "Custom schedule"}
                </p>
              </div>

              <div>
                <h3 className="text-sm font-medium">HTTP Request</h3>
                <div className="mt-2 rounded-md bg-muted p-4">
                  <p className="font-mono text-sm">
                    {task.method} {task.endpoint}
                  </p>
                  {/* {headers.map((header, index) => (
                    <p key={index} className="font-mono text-sm mt-2">
                      {header.key}: {header.value}
                    </p>
                  ))} */}
                </div>
              </div>

              {task.body && (
                <div>
                  <h3 className="text-sm font-medium">Request Body</h3>
                  <div className="mt-2 rounded-md bg-muted p-4">
                    <pre className="text-xs overflow-auto">
                      {typeof task.body === "string" ? task.body : JSON.stringify(task.body, null, 2)}
                    </pre>
                  </div>
                </div>
              )}
            </CardContent>
            <CardFooter>
              <Button variant="outline" className="w-full" asChild>
                <Link href={`/tasks/${task.id}/edit`}>Edit Configuration</Link>
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>

        <TabsContent value="chain">
          <Card>
            <CardHeader>
              <CardTitle>Task Chain</CardTitle>
              <CardDescription>Tasks that are triggered based on the execution result</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {!task.chains || task.chains.length === 0 ? (
                <p className="text-sm text-muted-foreground">This task does not have any chains configured.</p>
              ) : (
                task.chains.map((chain: { statusCode: string | number | bigint | boolean | ReactElement<unknown, string | JSXElementConstructor<any>> | Iterable<ReactNode> | ReactPortal | Promise<string | number | bigint | boolean | ReactPortal | ReactElement<unknown, string | JSXElementConstructor<any>> | Iterable<ReactNode> | null | undefined> | null | undefined; nextTaskName: any; nextTaskId: string | number | bigint | boolean | ReactElement<unknown, string | JSXElementConstructor<any>> | Iterable<ReactNode> | ReactPortal | Promise<string | number | bigint | boolean | ReactPortal | ReactElement<unknown, string | JSXElementConstructor<any>> | Iterable<ReactNode> | null | undefined> | null | undefined }, index: Key | null | undefined) => (
                  <div key={index} className="rounded-md border border-border p-4">
                    <div className="flex items-center justify-between">
                      <div className="space-y-2">
                        <div className="flex items-center gap-2">
                          <Badge variant="outline">Status: {chain.statusCode}</Badge>
                          <span className="text-sm font-medium">→ {chain.nextTaskName || "Unknown Task"}</span>
                        </div>
                        <p className="text-xs text-muted-foreground">Task ID: {chain.nextTaskId}</p>
                      </div>
                      <Link href={`/tasks/${chain.nextTaskId}`}>
                        <Button variant="ghost" size="sm">
                          View Task
                        </Button>
                      </Link>
                    </div>
                  </div>
                ))
              )}
            </CardContent>
            {task.chains && task.chains.length > 0 && (
              <CardFooter>
                <Button className="w-full" asChild>
                  <Link href={`/tasks/${task.id}/chain`}>Visualize Task Chain</Link>
                </Button>
              </CardFooter>
            )}
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
