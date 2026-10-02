"use client"

import CodeMirror from "@uiw/react-codemirror"
import { python } from "@codemirror/lang-python"
import type { Extension } from "@codemirror/state"

/**
 * Editor languages.
 *
 * Python is the only one with a grammar and the only one the backend can execute, but nothing here
 * is written around that. A language without a grammar yet resolves to an empty extension list and
 * still renders a usable editor, and adding one is an entry in this map plus a package — never a
 * change to the component's shape or to its callers.
 */
export type EditorLanguage = "python" | "plaintext"

const GRAMMARS: Record<EditorLanguage, Extension[]> = {
  python: [python()],
  plaintext: [],
}

export interface CodeEditorProps {
  language: EditorLanguage
  value: string
  onChange: (value: string) => void
  /** Accessible name. The editor is a labelled control and nothing else names it. */
  label: string
  readOnly?: boolean
  /** Rows tall. The default suits a first problem; nothing here is tall for effect. */
  rows?: number
  placeholder?: string
}

/**
 * The surface's own props, which add the one thing the caller must not supply.
 *
 * The theme comes from the document rather than from a prop at the call site, so it is kept off the
 * public type. A caller who could pass a theme would eventually pass one that disagreed with the
 * rest of the page, and the editor would be the one part of it in the wrong theme.
 */
export interface CodeEditorSurfaceProps extends CodeEditorProps {
  theme: "light" | "dark"
}

/**
 * The CodeMirror surface.
 *
 * Only reached through `code-editor.tsx`, which supplies `theme` from the document. Reading the
 * class here instead would mean reading localStorage during render and risking a mismatch with
 * the server's markup.
 */
export function CodeEditorSurface({
  language,
  value,
  onChange,
  label,
  theme,
  readOnly = false,
  rows = 14,
  placeholder,
}: CodeEditorSurfaceProps) {
  return (
    <CodeMirror
      value={value}
      height={`${Math.round(rows * 1.5) + 2}rem`}
      theme={theme}
      editable={!readOnly}
      readOnly={readOnly}
      placeholder={placeholder}
      onChange={onChange}
      extensions={GRAMMARS[language] ?? []}
      basicSetup={{
        lineNumbers: true,
        foldGutter: false,
        highlightActiveLine: !readOnly,
        highlightActiveLineGutter: !readOnly,
        // Autocompletion is off on purpose. The product's guidance is the hint and the walkthrough;
        // an editor that guesses the answer is a different product.
        autocompletion: false,
        searchKeymap: false,
      }}
      // Tab indents instead of leaving the field, which is what an editor is expected to do and
      // is most of what makes writing Python tolerable.
      indentWithTab
      onCreateEditor={(view) => {
        // The animation stage pages through steps on the arrow keys and space. Without this,
        // typing an arrow key inside a string literal jumps the walkthrough.
        view.dom.addEventListener("keydown", (event) => {
          const target = event.target as HTMLElement | null
          if (target && ["INPUT", "TEXTAREA", "BUTTON"].includes(target.tagName)) return
          if (["ArrowLeft", "ArrowRight", " ", "Spacebar"].includes(event.key)) {
            event.stopPropagation()
          }
        })
        view.dom.setAttribute("aria-label", label)
      }}
    />
  )
}