"use server"

import { getServerAuthToken } from "@/lib/auth-utils.server"
import { revalidatePath } from "next/cache"

// Base URL for the Task Management service
const TASK_SERVICE_URL = process.env.TASK_MANAGEMENT_SERVICE_URL || "http://localhost:8081"

// Function to fetch all tasks
export async function getAllTasks() {
  const token = await getServerAuthToken()
  console.log("I am called");
  
  if (!token) {
    throw new Error("Authentication required")
  }

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      cache: "no-store", // Don't cache this request
    })
    console.log("response",response);
    

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to fetch tasks: ${errorText}`)
    }

    return await response.json()
  } catch (error) {
    console.error("Error fetching tasks:", error)
    throw error
  }
}

// Function to fetch a single task by ID
export async function getTask(taskId: string) {
  const token = await getServerAuthToken()

  if (!token) {
    throw new Error("Authentication required")
  }

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks/${taskId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      cache: "no-store", // Don't cache this request
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to fetch task: ${errorText}`)
    }

    return await response.json()
  } catch (error) {
    console.error("Error fetching task:", error)
    throw error
  }
}

// Function to create a new task
export async function createTask(taskData: any) {
  const token = await getServerAuthToken()

  if (!token) {
    throw new Error("Authentication required")
  }

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(taskData),
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to create task: ${errorText}`)
    }

    // Revalidate the tasks list page to reflect the new task
    revalidatePath("/tasks")

    return await response.json()
  } catch (error) {
    console.error("Error creating task:", error)
    throw error
  }
}

// Function to update an existing task
export async function updateTask(taskId: string, taskData: any) {
  const token = await getServerAuthToken()

  if (!token) {
    throw new Error("Authentication required")
  }

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks/${taskId}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(taskData),
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to update task: ${errorText}`)
    }

    // Revalidate the tasks list and task detail pages
    revalidatePath("/tasks")
    revalidatePath(`/tasks/${taskId}`)

    return await response.json()
  } catch (error) {
    console.error("Error updating task:", error)
    throw error
  }
}

// Function to delete a task by ID
export async function deleteTask(taskId: string) {
  const token = await getServerAuthToken()

  if (!token) {
    throw new Error("Authentication required")
  }

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks/${taskId}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to delete task: ${errorText}`)
    }

    // Revalidate the tasks list page to reflect the deletion
    revalidatePath("/tasks")

    return { success: true }
  } catch (error) {
    console.error("Error deleting task:", error)
    throw error
  }
}

// Function to toggle task status (pause/resume)
export async function toggleTaskStatus(taskId: string, newStatus: "ACTIVE" | "PAUSED") {
  const token = await getServerAuthToken()

  if (!token) {
    throw new Error("Authentication required")
  }

  try {
    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks/${taskId}/status`, {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ status: newStatus }),
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to update task status: ${errorText}`)
    }

    // Revalidate the tasks list and task detail pages
    revalidatePath("/tasks")
    revalidatePath(`/tasks/${taskId}`)

    return { success: true }
  } catch (error) {
    console.error("Error updating task status:", error)
    throw error
  }
}
