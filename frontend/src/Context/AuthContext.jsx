import { createContext, useContext } from 'react';

const AuthContext = createContext({
    isAuthenticated: false,
    setIsAuthenticated: () => {},
});

export const useAuthContext = () => useContext(AuthContext);

export default AuthContext;