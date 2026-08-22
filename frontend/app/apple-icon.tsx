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
        {/* Pas de cercle : le fond orange plein du tile EST le ballon (iOS
            arrondit lui-même les coins), coutures + check en creux dessus —
            même famille que LogoSymbol variant="compact". */}
        <svg width="132" height="132" viewBox="0 0 120 120" fill="none">
          <path d="M28,54 Q60,64 92,54" stroke="#0B1120" strokeWidth={3} opacity={0.5} />
          <path d="M60,26 Q50,60 60,94" stroke="#0B1120" strokeWidth={3} opacity={0.5} />
          <path
            d="M21.5,59.4 L43.1,81 L53.3,80.6 L99.9,27.4 L89.3,18.1 L42.7,71.4 L53,71.1 L31.4,49.5 Z"
            fill="#F1F5F9"
          />
        </svg>
      </div>
    ),
    { ...size }
  );
}
