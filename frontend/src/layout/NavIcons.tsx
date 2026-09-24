import type { SVGProps } from 'react'

function Icon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      fill="none"
      height="22"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
      width="22"
      {...props}
    />
  )
}

export function LocationIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <path d="M12 21s-7-6.1-7-11.5A7 7 0 0 1 19 9.5C19 14.9 12 21 12 21Z" />
      <circle cx="12" cy="9.5" r="2.5" />
    </Icon>
  )
}

export function CatalogIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <path d="M4 9h13a3 3 0 0 1 0 6h-1" />
      <path d="M4 9v7a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3V9" />
      <path d="M7 3v2M11 3v2M15 3v2" />
    </Icon>
  )
}

export function CartIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <path d="M6 8h12l-1.2 10.2a2 2 0 0 1-2 1.8H9.2a2 2 0 0 1-2-1.8L6 8Z" />
      <path d="M9 8V6a3 3 0 0 1 6 0v2" />
    </Icon>
  )
}

export function ProfileIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M4.5 20c1.3-3.6 4.2-5.5 7.5-5.5s6.2 1.9 7.5 5.5" />
    </Icon>
  )
}
