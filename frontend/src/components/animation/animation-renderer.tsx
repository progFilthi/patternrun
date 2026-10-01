import type { ReactNode } from "react"

import type { AnimationStepType } from "@/types/api"
import type { RendererProps } from "@/components/animation/types"
import { ArrayRenderer } from "@/components/animation/renderers/array-renderer"
import { CodeRenderer } from "@/components/animation/renderers/code-renderer"
import { DpTableRenderer } from "@/components/animation/renderers/dp-table-renderer"
import { GraphRenderer } from "@/components/animation/renderers/graph-renderer"
import { HashMapRenderer } from "@/components/animation/renderers/hash-map-renderer"
import { HeapRenderer } from "@/components/animation/renderers/heap-renderer"
import { IntervalRenderer } from "@/components/animation/renderers/interval-renderer"
import { PointerRenderer } from "@/components/animation/renderers/pointer-renderer"
import { PrefixSumRenderer } from "@/components/animation/renderers/prefix-sum-renderer"
import { QuestionRenderer } from "@/components/animation/renderers/question-renderer"
import { StackRenderer } from "@/components/animation/renderers/stack-renderer"
import { StatusRenderer } from "@/components/animation/renderers/status-renderer"
import { TextRenderer } from "@/components/animation/renderers/text-renderer"
import { TreeRenderer } from "@/components/animation/renderers/tree-renderer"
import { WindowRenderer } from "@/components/animation/renderers/window-renderer"
import { EmptyPayload } from "@/components/animation/primitives"

/**
 * The whole point of the renderer: animation steps are data, so one registry maps a step type
 * to a component. No per problem animation code exists anywhere in the app.
 */
const RENDERERS: Record<AnimationStepType, (props: RendererProps) => ReactNode> = {
  ARRAY: (props) => <ArrayRenderer {...props} />,
  POINTER: (props) => <PointerRenderer {...props} />,
  WINDOW: (props) => <WindowRenderer {...props} />,
  HASH_MAP: (props) => <HashMapRenderer {...props} />,
  STACK: (props) => <StackRenderer {...props} />,
  HEAP: (props) => <HeapRenderer {...props} />,
  TREE: (props) => <TreeRenderer {...props} />,
  GRAPH: (props) => <GraphRenderer {...props} />,
  GRID: (props) => <GraphRenderer {...props} />,
  INTERVAL: (props) => <IntervalRenderer {...props} />,
  PREFIX_SUM: (props) => <PrefixSumRenderer {...props} />,
  DP_TABLE: (props) => <DpTableRenderer {...props} />,
  CODE: (props) => <CodeRenderer {...props} />,
  TEXT: (props) => <TextRenderer {...props} />,
  QUESTION: (props) => <QuestionRenderer {...props} />,
  SUCCESS: (props) => <StatusRenderer {...props} tone="success" />,
  FAILURE: (props) => <StatusRenderer {...props} tone="failure" />,
}

export function AnimationRenderer({ type, ...props }: RendererProps & { type: AnimationStepType }) {
  const renderer = RENDERERS[type]
  if (!renderer) {
    return <EmptyPayload message={`No renderer is registered for step type "${type}".`} />
  }
  if (Object.keys(props.payload).length === 0 && type !== "SUCCESS" && type !== "FAILURE") {
    return <EmptyPayload />
  }
  return renderer(props)
}