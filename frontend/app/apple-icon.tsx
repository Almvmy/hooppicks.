import { ImageResponse } from "next/og";

export const size = { width: 180, height: 180 };
export const contentType = "image/png";

export default function AppleIcon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "#FF7A1A",
        }}
      >
        <svg width="132" height="132" viewBox="0 0 120 120" fill="none">
          <circle cx="58" cy="62" r="42" stroke="#0B1120" strokeWidth={7} />
          <path d="M16 62 H100" stroke="#0B1120" strokeWidth={5} />
          <path d="M58 20 V104" stroke="#0B1120" strokeWidth={5} />
          <path
            d="M32 66 L54 88 L108 16"
            stroke="#FF7A1A"
            strokeWidth={24}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M32 66 L54 88 L108 16"
            stroke="#0B1120"
            strokeWidth={12}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </div>
    ),
    { ...size }
  );
}
