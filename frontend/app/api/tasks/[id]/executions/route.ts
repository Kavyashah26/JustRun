import { NextResponse } from "next/server"
import { getServerAuthToken } from "@/lib/auth-utils.server"

export async function GET(request: Request, { params }: { params: { id: string } }) {
  const token = await getServerAuthToken()

  if (!token) {
    return NextResponse.json({ error: "Authentication required" }, { status: 401 })
  }

  const { id } = await params

  const { searchParams } = new URL(request.url)
  const page = searchParams.get("page") || "1"
  const limit = searchParams.get("limit") || "10"

  try {
    const TASK_SERVICE_URL = process.env.TASK_MANAGEMENT_SERVICE_URL || "http://localhost:8081"

    const response = await fetch(`${TASK_SERVICE_URL}/api/tasks/`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      cache: "no-store",
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to fetch task executions: ${errorText}`)
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error fetching task executions:", error)
    return NextResponse.json({ error: "Failed to fetch task executions" }, { status: 500 })
  }
}
