// Géométrie du logo HoopPicks (le tir qui entre dans le panier).
// Généré depuis le kit logo (docs/logo) : ne pas retoucher à la main,
// refaire l'export à partir du kit si le dessin change.
// Deux déclinaisons : « full » (avec le tir en pointillés, à partir de ~64 px)
// et « small » (filet épais, sans pointillés, lisible jusqu'à 16 px).

export type LogoGeometry = {
  viewBox: string;
  dots: [number, number, number][];
  backRim: string;
  frontRim: string;
  ball: { cx: number; cy: number; r: number };
  seams: string[];
  net: string;
  edge: string;
  rimW: number;
  backRimW: number;
  netW: number;
  seamW: number;
};

export const LOGO_FULL: LogoGeometry = {
  "viewBox": "65.5 106.0 339.0 333.8",
  "dots": [
    [
      71.3,
      228.9,
      3.6
    ],
    [
      76.8,
      195.8,
      4.1
    ],
    [
      85.8,
      168.0,
      4.8
    ],
    [
      97.6,
      145.9,
      5.5
    ],
    [
      111.6,
      129.6,
      6.2
    ],
    [
      126.9,
      119.5,
      7.0
    ],
    [
      143.1,
      115.8,
      7.8
    ],
    [
      159.4,
      118.8,
      8.6
    ],
    [
      175.0,
      128.8,
      9.5
    ],
    [
      189.4,
      146.1,
      10.3
    ],
    [
      201.9,
      170.8,
      11.2
    ]
  ],
  "backRim": "M142 300A126 28 0 0 1 394 300",
  "frontRim": "M142 300A126 28 0 0 0 394 300",
  "ball": {
    "cx": 268,
    "cy": 258,
    "r": 78
  },
  "seams": [
    "M268 180V336",
    "M190 258H346",
    "M223.0 213.0Q262.1 258 223.0 303.0",
    "M313.0 213.0Q273.9 258 313.0 303.0"
  ],
  "net": "M142.0 300.0L230.8 431.2M192.4 322.4L206.0 420.0M192.4 322.4L255.6 433.7M242.8 327.4L230.8 431.2M242.8 327.4L280.4 433.7M293.2 327.4L255.6 433.7M293.2 327.4L305.2 431.2M343.6 322.4L280.4 433.7M343.6 322.4L330.0 420.0M394.0 300.0L305.2 431.2",
  "edge": "M206.0 420.0A62 14 0 0 0 330.0 420.0",
  "rimW": 17,
  "backRimW": 14,
  "netW": 7.5,
  "seamW": 7.5
};

export const LOGO_SMALL: LogoGeometry = {
  "viewBox": "90.0 122.0 332.0 338.0",
  "dots": [],
  "backRim": "M108 300A148 32 0 0 1 404 300",
  "frontRim": "M108 300A148 32 0 0 0 404 300",
  "ball": {
    "cx": 256,
    "cy": 236,
    "r": 112
  },
  "seams": [
    "M256 124V348",
    "M144 236H368",
    "M191.4 171.4Q247.6 236 191.4 300.6",
    "M320.6 171.4Q264.4 236 320.6 300.6"
  ],
  "net": "M108.0 300.0L228.0 445.1M206.7 330.2L172.0 430.0M206.7 330.2L284.0 445.1M305.3 330.2L228.0 445.1M305.3 330.2L340.0 430.0M404.0 300.0L284.0 445.1",
  "edge": "M172.0 430.0A84 16 0 0 0 340.0 430.0",
  "rimW": 32,
  "backRimW": 29,
  "netW": 24,
  "seamW": 16
};
// « medium » : la version des en-têtes (36 à 72 px) du kit, reprise de
// docs/logo/svg/hooppicks-mark-medium.svg : pointillés plus gros et moins
// nombreux, filet et jante épaissis, pour rester lisible à 40-48 px.
export const LOGO_MEDIUM: LogoGeometry = {
  viewBox: "42.0 82.8 387.0 380.8",
  dots: [
    [71.3, 228.9, 5.2],
    [80.3, 183.2, 6.1],
    [95.8, 148.7, 7.1],
    [115.8, 126.1, 8.2],
    [138.4, 116.2, 9.4],
    [161.7, 119.8, 10.6],
    [183.5, 137.8, 11.9],
    [201.9, 170.8, 13.2],
  ],
  backRim: "M142 300A126 28 0 0 1 394 300",
  frontRim: "M142 300A126 28 0 0 0 394 300",
  ball: { cx: 268, cy: 258, r: 78 },
  seams: [
    "M268 180V336",
    "M190 258H346",
    "M223.0 213.0Q262.1 258 223.0 303.0",
    "M313.0 213.0Q273.9 258 313.0 303.0",
  ],
  net: "M142.0 300.0L237.0 432.1M205.0 324.2L206.0 420.0M205.0 324.2L268.0 434.0M268.0 328.0L237.0 432.1M268.0 328.0L299.0 432.1M331.0 324.2L268.0 434.0M331.0 324.2L330.0 420.0M394.0 300.0L299.0 432.1",
  edge: "M206.0 420.0A62 14 0 0 0 330.0 420.0",
  rimW: 22,
  backRimW: 19,
  netW: 11,
  seamW: 10,
};
