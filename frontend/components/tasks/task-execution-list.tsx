"use client"

import { useEffect, useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { Skeleton } from "@/components/ui/skeleton"
import { AlertCircle, ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

interface Execution {
  id: string
  executionTime: string
  status: string
  statusCode: number
  response: string
  error: string | null
  retryCount: number
  nextRetry: string | null
}

export function TaskExecutionsList({ taskId }: { taskId: string }) {
  const [executions, setExecutions] = useState<Execution[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const itemsPerPage = 10
  console.log("TaskExecutions component is rendering");

  useEffect(() => {
      console.log("Hello");
    async function fetchExecutions() {
      setIsLoading(true)
      setError(null)

      try {
        const response = await fetch(`/api/tasks/${taskId}/executions?page=${page}&limit=${itemsPerPage}`)

        if (!response.ok) {
          throw new Error(`Failed to fetch executions: ${response.statusText}`)
        }

        const data = await response.json()
        
        // Check if we got fewer items than requested, which means we're at the end
        setHasMore(data.length === itemsPerPage)
        setExecutions(data || [])
      } catch (err) {
        console.error("Error fetching executions:", err)
        setError(err instanceof Error ? err.message : "Failed to load executions")
      } finally {
        setIsLoading(false)
      }
    }

    fetchExecutions()
  }, [taskId, page])

  // Helper function to get appropriate badge variant based on status
  const getStatusVariant = (status: string) => {
    switch (status.toUpperCase()) {
      case "COMPLETED":
        return "outline"
      case "FAILED":
        return "destructive"
      case "PENDING":
        return "secondary"
      default:
        return "default"
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Execution History</CardTitle>
        <CardDescription>Complete history of task executions</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-4">
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          </div>
        ) : error ? (
          <div className="py-8 text-center">
            <AlertCircle className="h-10 w-10 text-destructive mx-auto mb-4" />
            <p className="text-muted-foreground">{error}</p>
            <Button variant="outline" className="mt-4" onClick={() => window.location.reload()}>
              Retry
            </Button>
          </div>
        ) : executions.length === 0 ? (
          <div className="py-8 text-center">
            <p className="text-muted-foreground">No executions found for this task</p>
          </div>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Execution ID</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Status Code</TableHead>
                  <TableHead>Execution Time</TableHead>
                  <TableHead>Retry Count</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {executions.map((execution) => (
                  <TableRow key={execution.id}>
                    <TableCell className="font-mono text-xs">{execution.id}</TableCell>
                    <TableCell>
                      <Badge variant={getStatusVariant(execution.status)} className="capitalize">
                        {execution.status.toLowerCase()}
                      </Badge>
                    </TableCell>
                    <TableCell>{execution.statusCode}</TableCell>
                    <TableCell>{new Date(execution.executionTime).toLocaleString()}</TableCell>
                    <TableCell>{execution.retryCount}</TableCell>
                    <TableCell className="text-right">
                      <Link href={`/executions/${execution.id}`}>
                        <Button variant="ghost" size="sm">
                          View Details
                        </Button>
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            <div className="flex items-center justify-end space-x-2 py-4 mt-4">
              <Button variant="outline" size="sm" onClick={() => setPage(page - 1)} disabled={page === 1}>
                <ChevronLeftIcon className="h-4 w-4" />
              </Button>
              <Button variant="outline" size="sm" disabled>
                Page {page}
              </Button>
              <Button variant="outline" size="sm" onClick={() => setPage(page + 1)} disabled={!hasMore}>
                <ChevronRightIcon className="h-4 w-4" />
              </Button>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}
