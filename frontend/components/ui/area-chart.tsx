"use client";

import { useId } from "react";

interface AreaChartProps {
  values: number[];
  width?: number;
  height?: number;
  className?: string;
  strokeClassName?: string;
  gradientFromClassName?: string;
}

/**
 * Petit graphique en aire, sans dépendance externe (pur SVG).
 * `values` doit être trié chronologiquement (du plus ancien au plus récent).
 */
export function AreaChart({
  values,
  width = 400,
  height = 96,
  className,
  strokeClassName = "stroke-primary",
  gradientFromClassName = "text-primary",
}: AreaChartProps) {
  const gradientId = useId();

  if (values.length < 2) {
    return <div style={{ width, height }} className={className} />;
  }

  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const padding = 4;

  const stepX = (width - padding * 2) / (values.length - 1);
  const points = values.map((v, i) => {
    const x = padding + i * stepX;
    const y = padding + (1 - (v - min) / range) * (height - padding * 2);
    return [x, y] as const;
  });

  const linePath = points
    .map(([x, y], i) => `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`)
    .join(" ");

  const areaPath = `${linePath} L${points[points.length - 1][0].toFixed(2)},${height} L${points[0][0].toFixed(2)},${height} Z`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width="100%"
      height={height}
      preserveAspectRatio="none"
      className={className}
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="currentColor" stopOpacity="0.35" className={gradientFromClassName} />
          <stop offset="100%" stopColor="currentColor" stopOpacity="0" className={gradientFromClassName} />
        </linearGradient>
      </defs>
      <path d={areaPath} fill={`url(#${gradientId})`} stroke="none" />
      <path
        d={linePath}
        fill="none"
        strokeWidth={2}
        className={strokeClassName}
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle
        cx={points[points.length - 1][0]}
        cy={points[points.length - 1][1]}
        r={3.5}
        className={strokeClassName}
        fill="currentColor"
      />
    </svg>
  );
}
