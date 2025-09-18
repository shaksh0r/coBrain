import React from 'react';
import { useIDEContext } from '../../Context/IDEContext';
import { getContainer } from '../../API/container';

const ContainerButton = () => {
    const { sessionID, language } = useIDEContext();

    const handleClick = async () => {
        await getContainer(sessionID, language);
    };

    return (
        <button
            onClick={handleClick}
            style={{
                background: '#333',
                color: 'white',
                border: 'none',
                padding: '4px 8px',
                marginRight: '8px',
            }}
        >
            Run Container
        </button>
    );
};

export default ContainerButton;