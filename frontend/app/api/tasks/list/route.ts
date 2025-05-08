import { NextResponse } from "next/server"
import { getAllTasks } from "@/lib/task-service"

export async function GET() {
  try {
    console.log("In get");
    
    const tasks = await getAllTasks()
    return NextResponse.json(tasks)
  } catch (error) {
    console.error("Error fetching tasks:", error)
    return NextResponse.json({ error: "Failed to fetch tasks" }, { status: 500 })
  }
}
