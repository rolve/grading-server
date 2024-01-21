/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/main/resources/**/*.{peb,html}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Mulish', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace']
      },
    },
  },
  plugins: [],
}
