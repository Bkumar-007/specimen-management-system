import "./globals.css";

export const metadata = {
  title: "Specimen Management System",
  description: "Lab specimen workflow: orders, accession, status, results",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
