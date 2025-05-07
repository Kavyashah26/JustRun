import { cookies } from "next/headers"
import { redirect } from "next/navigation"

// Server-side function to check if user is authenticated
export async function requireAuth() {
  const cookieStore = cookies()
  const token = (await cookieStore).get("auth_token")
  
  if (!token) {
    redirect("/login")
  }
  
  return token.value
}

// Server-side function to get auth token
export async function getServerAuthToken() {
  const cookieStore = cookies()
  return (await cookieStore).get("auth_token")?.value
}
