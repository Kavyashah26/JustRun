// "use client"

// import { useState, useEffect } from "react"
// import { useTheme } from "next-themes"
// import { Button } from "@/components/ui/button"
// import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
// import { BellIcon, MoonIcon, SunIcon, UserIcon } from "lucide-react"
// import { Badge } from "@/components/ui/badge"
// import { logout } from "@/lib/auth-service"

// export function Header() {
//   const { setTheme } = useTheme()
//   const [mounted, setMounted] = useState(false)

//   // Avoid hydration mismatch
//   useEffect(() => {
//     setMounted(true)
//   }, [])

//   const handleLogout = () => {
//     logout()
//   }

//   if (!mounted) {
//     return (
//       <header className="sticky top-0 z-10 flex h-16 items-center gap-4 border-b border-border bg-background px-4 md:px-6">
//         <div className="ml-auto flex items-center gap-4">
//           <div className="h-8 w-8 rounded-full bg-muted" />
//         </div>
//       </header>
//     )
//   }

//   return (
//     <header className="sticky top-0 z-10 flex h-16 items-center gap-4 border-b border-border bg-background px-4 md:px-6">
//       <div className="ml-auto flex items-center gap-4">
//         <Button variant="outline" size="icon" className="relative">
//           <BellIcon className="h-5 w-5" />
//           <Badge className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-xs">
//             3
//           </Badge>
//         </Button>

//         <DropdownMenu>
//           <DropdownMenuTrigger asChild>
//             <Button variant="outline" size="icon">
//               <SunIcon className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
//               <MoonIcon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
//               <span className="sr-only">Toggle theme</span>
//             </Button>
//           </DropdownMenuTrigger>
//           <DropdownMenuContent align="end">
//             <DropdownMenuItem onClick={() => setTheme("light")}>Light</DropdownMenuItem>
//             <DropdownMenuItem onClick={() => setTheme("dark")}>Dark</DropdownMenuItem>
//             <DropdownMenuItem onClick={() => setTheme("system")}>System</DropdownMenuItem>
//           </DropdownMenuContent>
//         </DropdownMenu>

//         <DropdownMenu>
//           <DropdownMenuTrigger asChild>
//             <Button variant="outline" size="icon">
//               <UserIcon className="h-5 w-5" />
//             </Button>
//           </DropdownMenuTrigger>
//           <DropdownMenuContent align="end">
//             <DropdownMenuItem>Profile</DropdownMenuItem>
//             <DropdownMenuItem>Settings</DropdownMenuItem>
//             <DropdownMenuItem onClick={handleLogout}>Logout</DropdownMenuItem>
//           </DropdownMenuContent>
//         </DropdownMenu>
//       </div>
//     </header>
//   )
// }
