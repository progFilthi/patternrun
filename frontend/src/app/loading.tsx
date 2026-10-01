import { Skeleton } from "@/components/ui/skeleton"

export default function Loading() {
  return (
    <div className="mx-auto w-full max-w-5xl px-6 pt-14">
      <Skeleton className="h-3 w-32" />
      <Skeleton className="mt-4 h-10 w-2/3" />
      <Skeleton className="mt-5 h-4 w-full max-w-2xl" />
      <div className="mt-12 flex flex-col gap-4">
        {Array.from({ length: 6 }).map((_, index) => (
          <Skeleton key={index} className="h-10 w-full" />
        ))}
      </div>
    </div>
  )
}
