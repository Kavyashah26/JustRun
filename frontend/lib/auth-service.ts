"use client"

// Replace with your actual auth service URL
const AUTH_SERVICE_URL = process.env.NEXT_PUBLIC_AUTH_SERVICE_URL || "http://localhost:8080/api/auth"

export interface AuthResponse {
  token: string
  userId: string
  username: string
  email: string
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${AUTH_SERVICE_URL}/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ username, password }),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.message || "Failed to login")
  }

  const data: AuthResponse = await response.json()
  
  // Set the token in a cookie
  document.cookie = `auth_token=${data.token}; path=/; max-age=86400; SameSite=Strict`
  
  return data
}

export async function logout() {
  document.cookie = "auth_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT"
  window.location.href = "/login"
}

export function getAuthToken(): string | null {
  const cookies = document.cookie.split(';')
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=')
    if (name === 'auth_token') {
      return value
    }
  }
  return null
}
