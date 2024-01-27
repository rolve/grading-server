/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/**/*.{peb,html}",
    "./node_modules/flowbite/**/*.js"
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Mulish', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace']
      },
    },
  },
  plugins: [
    require('flowbite/plugin')({
      charts: false,
      forms: false,
      tooltips: false
    }),
  ],
}
