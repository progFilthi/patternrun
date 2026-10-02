"use client"

import { createContext, useContext, useEffect, useMemo, useState } from "react"

import { ensureSession, SessionApiError } from "@/lib/api/session"
import type { AuthResponse } from "@/types/api"

/**
 * Establishes the learner's session once, on first load.
 *
 * Nothing else in the app creates a session, so there is exactly one place where identity comes
 * into being and exactly one place to look when it fails. A visitor is never blocked: an
 * anonymous learner is created on demand and the app carries on whether or not that call
 * succeeds, because refusing to render over a failed session would be worse than the failure.
 */

export type SessionStatus = "establishing" | "ready" | "anonymous-ready" | "unavailable"

interface SessionContextValue {
  status: SessionStatus
  user?: AuthResponse["user"]
  /** Set when the session call failed, so a write can explain itself rather than hang. */
  error?: string
}

const SessionContext = createContext<SessionContextValue>({ status: "establishing" })

export function useSession(): SessionContextValue {
  return useContext(SessionContext)
}

/** The learner, or undefined while unknown. Null-safe for anonymous use. */
export function useLearnerId(): string | undefined {
  return useSession().user?.id
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<SessionContextValue>({ status: "establishing" })

  useEffect(() => {
    // A cancelled effect must not leave a session call's result dangling.
    let live = true

    ensureSession()
      .then((response) => {
        if (!live) return
        setState({
          status: response.user.anonymous ? "anonymous-ready" : "ready",
          user: response.user,
        })
      })
      .catch((error: unknown) => {
        if (!live) return
        setState({
          status: "unavailable",
          error:
            error instanceof SessionApiError
              ? error.message
              : "Could not start a session.",
        })
      })

    return () => {
      live = false
    }
  }, [])

  const value = useMemo(() => state, [state])
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}