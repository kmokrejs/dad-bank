import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useMe } from '../features/auth/queries'
import { PageSpinner } from '../components/ui/Spinner'

export function RequireAuth({ role }: { role?: 'ADMIN' }) {
  const { data: user, isPending } = useMe()
  const location = useLocation()
  if (isPending) return <PageSpinner />
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (role && user.role !== role) return <Navigate to="/" replace />
  return <Outlet />
}
