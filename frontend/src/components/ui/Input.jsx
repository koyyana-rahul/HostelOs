import { forwardRef } from "react";
import { cn } from "../../utils/cn.js";

const baseInputClassName =
  "h-10 w-full rounded-lg border bg-surface px-3 py-2 text-sm text-ink transition-colors placeholder:text-muted/70 focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted disabled:border-line/60";

const defaultBorderClassName =
  "border-line hover:border-brand/30 focus:border-brand focus:ring-brand/15";

const invalidBorderClassName =
  "border-danger text-ink focus:border-danger focus:ring-danger/15";

export const Input = forwardRef(function Input(
  {
    type = "text",
    invalid = false,
    disabled = false,
    className,
    "aria-invalid": ariaInvalid,
    ...props
  },
  ref,
) {
  const isInvalid = Boolean(
    invalid || ariaInvalid === true || ariaInvalid === "true",
  );

  return (
    <input
      ref={ref}
      type={type}
      disabled={disabled}
      aria-invalid={isInvalid ? true : undefined}
      className={cn(
        baseInputClassName,
        isInvalid ? invalidBorderClassName : defaultBorderClassName,
        className,
      )}
      {...props}
    />
  );
});
