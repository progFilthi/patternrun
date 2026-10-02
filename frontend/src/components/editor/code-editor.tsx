"use client"

import dynamic from "next/dynamic"

import type { CodeEditorProps } from "./code-editor-surface"
import { useDarkTheme } from "@/lib/use-dark-theme"

/**
 * CodeMirror touches `document` while it mounts, so it cannot be rendered on the server.
 *
 * Splitting it out behind `dynamic` is what keeps that detail in one place. Everything above this
 * component stays an ordinary server-safe client component, and the loading state below is a plain
 * box of the right height, so the page does not jump when the editor arrives.
 */
const Surface = dynamic(() => import("./code-editor-surface").then((m) => m.CodeEditorSurface), {
  ssr: false,
  loading: () => <EditorSkeleton />,
})

/**
 * A code editor.
 *
 * Isolated on purpose. It knows about syntax, theming and Tab, and nothing about problems,
 * attempts, running or scoring — the caller hands it a language and a string and gets a string back.
 * That is what lets the same component serve the learner's solution, the revealed reference and a
 * future Java editor without any of them growing a special case.
 *
 * CodeMirror rather than Monaco because the job here is reading and editing Python, not
 * intellisense. The diagnosis, the hint and the explanation are this product's actual teaching
 * surface, and a full language server inside the editor would be a large dependency competing with
 * them for attention and for bytes.
 */
export function CodeEditor(props: CodeEditorProps) {
  const dark = useDarkTheme()

  return (
    <div
      className="overflow-hidden rounded-md border bg-code"
      // The editor is a labelled control and nothing else names it.
      role="group"
      aria-label={props.label}
      data-language={props.language}
      data-theme={dark ? "dark" : "light"}
    >
      <Surface {...props} theme={dark ? "dark" : "light"} />
    </div>
  )
}

export type { CodeEditorProps, EditorLanguage } from "./code-editor-surface"

/** The same box the editor will fill, so nothing shifts when it loads. */
function EditorSkeleton() {
  return (
    <div className="flex flex-col gap-2 p-4" aria-hidden>
      {Array.from({ length: 5 }, (_, index) => (
        <div
          key={index}
          className="h-3 rounded-sm bg-muted"
          style={{ width: `${92 - index * 11}%` }}
        />
      ))}
    </div>
  )
}