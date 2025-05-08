"use client"

import { useState, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Label } from "@/components/ui/label"
import { SearchIcon } from "lucide-react"
import { useRouter, useSearchParams } from "next/navigation"

export function TasksFilter() {
  const router = useRouter()
  const searchParams = useSearchParams()

  // Initialize state from URL params
  const [search, setSearch] = useState(searchParams.get("search") || "")
  const [statusFilters, setStatusFilters] = useState({
    active: searchParams.get("status")?.includes("active") || false,
    paused: searchParams.get("status")?.includes("paused") || false,
    error: searchParams.get("status")?.includes("error") || false,
  })
  const [priorityFilters, setPriorityFilters] = useState({
    high: searchParams.get("priority")?.includes("high") || false,
    normal: searchParams.get("priority")?.includes("normal") || false,
    low: searchParams.get("priority")?.includes("low") || false,
  })
  const [hasChain, setHasChain] = useState(searchParams.get("hasChain") === "true" || false)

  // Memoize the applyFilters function to prevent recreating it on every render
  const applyFilters = useCallback(() => {
    // Build query parameters
    const params = new URLSearchParams()

    if (search) {
      params.set("search", search)
    }

    const statusValues = Object.entries(statusFilters)
      .filter(([_, isChecked]) => isChecked)
      .map(([status]) => status)

    if (statusValues.length > 0) {
      params.set("status", statusValues.join(","))
    }

    const priorityValues = Object.entries(priorityFilters)
      .filter(([_, isChecked]) => isChecked)
      .map(([priority]) => priority)

    if (priorityValues.length > 0) {
      params.set("priority", priorityValues.join(","))
    }

    if (hasChain) {
      params.set("hasChain", "true")
    }

    // Navigate with the new query parameters
    router.push(`/tasks?${params.toString()}`)
  }, [search, statusFilters, priorityFilters, hasChain, router])

  const resetFilters = useCallback(() => {
    setSearch("")
    setStatusFilters({ active: false, paused: false, error: false })
    setPriorityFilters({ high: false, normal: false, low: false })
    setHasChain(false)
    router.push("/tasks")
  }, [router])

  return (
    <Card>
      <CardHeader>
        <CardTitle>Filters</CardTitle>
        <CardDescription>Filter tasks by criteria</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="search">Search</Label>
          <div className="relative">
            <SearchIcon className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              id="search"
              type="search"
              placeholder="Search tasks..."
              className="pl-8"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label>Status</Label>
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="status-active"
                checked={statusFilters.active}
                onCheckedChange={(checked) => setStatusFilters({ ...statusFilters, active: checked === true })}
              />
              <Label htmlFor="status-active" className="text-sm font-normal">
                Active
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="status-paused"
                checked={statusFilters.paused}
                onCheckedChange={(checked) => setStatusFilters({ ...statusFilters, paused: checked === true })}
              />
              <Label htmlFor="status-paused" className="text-sm font-normal">
                Paused
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="status-error"
                checked={statusFilters.error}
                onCheckedChange={(checked) => setStatusFilters({ ...statusFilters, error: checked === true })}
              />
              <Label htmlFor="status-error" className="text-sm font-normal">
                Error
              </Label>
            </div>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Priority</Label>
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="priority-high"
                checked={priorityFilters.high}
                onCheckedChange={(checked) => setPriorityFilters({ ...priorityFilters, high: checked === true })}
              />
              <Label htmlFor="priority-high" className="text-sm font-normal">
                High
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="priority-normal"
                checked={priorityFilters.normal}
                onCheckedChange={(checked) => setPriorityFilters({ ...priorityFilters, normal: checked === true })}
              />
              <Label htmlFor="priority-normal" className="text-sm font-normal">
                Normal
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="priority-low"
                checked={priorityFilters.low}
                onCheckedChange={(checked) => setPriorityFilters({ ...priorityFilters, low: checked === true })}
              />
              <Label htmlFor="priority-low" className="text-sm font-normal">
                Low
              </Label>
            </div>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Has Chain</Label>
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="has-chain"
                checked={hasChain}
                onCheckedChange={(checked) => setHasChain(checked === true)}
              />
              <Label htmlFor="has-chain" className="text-sm font-normal">
                With task chain
              </Label>
            </div>
          </div>
        </div>

        <div className="space-y-2">
          <Button className="w-full" onClick={applyFilters}>
            Apply Filters
          </Button>
          <Button variant="outline" className="w-full" onClick={resetFilters}>
            Reset Filters
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
